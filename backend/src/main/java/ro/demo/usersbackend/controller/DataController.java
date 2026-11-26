package ro.demo.usersbackend.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.NativeQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import jakarta.persistence.metamodel.EntityType;
import ro.demo.usersbackend.core.persistence.SessionFactoryProvider;
import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;

@RestController
@RequestMapping("/api/data")
public class DataController {

	// Writes messages in the Eclipse console
	private static final Logger log = LoggerFactory.getLogger(DataController.class);

	// All rows of a table, plus the columns of the linked tables from entitySpec
	// e.g. GET /api/data/StockItem also brings ARTICLE_ID__BARCODE and
	// ARTICLE_ID__DESCRIPTION
	@GetMapping("/{entity}")
	public List<Map<String, Object>> findAll(@PathVariable String entity, @RequestParam Map<String, String> filters)
			throws IOException {
		// no dialog file (e.g. StorageLocation, used by the home page): just all the
		// rows
		log.info("Data: {} with filters {}", entity, filters);
		if (!new ClassPathResource("static/api/dialogs/" + entity + ".json").exists()) {
			return select("SELECT * FROM " + tableNameOf(entity), new ArrayList<>());
		}

		JsonNode columns = readJson(entity + ".json").get("columns");
		JsonNode entitySpec = readJson("dialogs.json").get("entitySpec");

		String select = "SELECT t.*";
		String joins = "";
		Map<String, String> names = new HashMap<>();

		for (JsonNode column : columns) {
			names.put(column.get("field").asText(), "t." + column.get("field").asText());

			if (column.has("lookup")) {
				String field = column.get("field").asText(); // ARTICLE_ID
				String linked = column.get("lookup").get("entity").asText(); // Article
				String j = "j_" + field; // a short name for the joined table

				joins = joins + " LEFT JOIN " + tableNameOf(linked) + " " + j + " ON " + j + ".ID = t." + field;

				for (JsonNode spec : entitySpec) {
					if (spec.get("name").asText().equals(linked)) {
						for (JsonNode show : spec.get("show")) {
							String name = show.get("field").asText(); // BARCODE
							if (show.has("from")) {
								String j2 = j + "_" + name;
								joins = joins + " LEFT JOIN " + tableNameOf(show.get("from").asText()) + " " + j2
										+ " ON " + j2 + ".ID = " + j + "." + name;
								select = select + ", " + j2 + "." + show.get("column").asText() + " AS " + field + "__"
										+ name;
								names.put(field + "__" + name, j2 + "." + show.get("column").asText());
							} else {
								select = select + ", " + j + "." + name + " AS " + field + "__" + name;
								names.put(field + "__" + name, j + "." + name);
							}
						}
					}
				}
			}
		}

		String where = " WHERE 1 = 1";
		List<Object> params = new ArrayList<>();
		for (String key : filters.keySet()) {
			if (names.containsKey(key) && !filters.get(key).isEmpty()) {
				where = where + " AND UPPER(" + names.get(key) + ") LIKE ?";
				params.add("%" + filters.get(key).toUpperCase() + "%");
			}
		}

		String paging = "";
		String sortField = names.get(filters.get("sort"));
		if (sortField == null) {
			sortField = "t.ID";
		}
		String direction = "desc".equalsIgnoreCase(filters.get("direction")) ? "DESC" : "ASC";
		String tieBreaker = "t.ID".equals(sortField) ? "" : ", t.ID ASC";
		if (filters.containsKey("page")) {
			select = select + ", COUNT(*) OVER () AS TOTAL_ROWS";
			paging = " ORDER BY " + sortField + " " + direction + tieBreaker + " OFFSET ? ROWS FETCH NEXT 20 ROWS ONLY";
			params.add(Integer.parseInt(filters.get("page")) * 20);
		} else {
			paging = " ORDER BY " + sortField + " " + direction + tieBreaker;
		}

		return select(select + " FROM " + tableNameOf(entity) + " t" + joins + where + paging, params);
	}

	// The columns of a table: name, type and if it can be empty
	@GetMapping("/{entity}/columns")
	public List<Map<String, Object>> columns(@PathVariable String entity) {
		log.info("Data: columns of table {}", entity);
		String sql = "SELECT COLUMN_NAME, DATA_TYPE, NULLABLE FROM USER_TAB_COLUMNS WHERE TABLE_NAME = ?";
		List<Object> params = new ArrayList<>();
		params.add(tableNameOf(entity).toUpperCase());
		return select(sql, params);
	}

	// Adds one row, e.g. POST /api/data/Destination with { "NAME": "Sibiu" }
	@PostMapping("/{entity}")
	public ResponseEntity<Map<String, String>> add(@PathVariable String entity,
			@RequestBody Map<String, Object> values) {
		log.info("Data: added a row to {} with columns {}", entity, values.keySet());
		if (entity.equals("Workplace")) {
			String taken = workplaceUsingIp(values.get("IP"), null);
			if (taken != null) {
				return ResponseEntity.badRequest().body(
						Map.of("message", "IP " + values.get("IP") + " is already assigned to workplace " + taken));
			}
		}
		List<String> names = new ArrayList<>();
		List<String> marks = new ArrayList<>();
		List<Object> params = new ArrayList<>();

		// only the columns that really exist in the table
		for (Map<String, Object> column : columns(entity)) {
			String name = (String) column.get("COLUMN_NAME");
			if (values.containsKey(name)) {
				names.add(name);
				marks.add("?");
				params.add(values.get(name));
			}
		}

		String sql = "INSERT INTO " + tableNameOf(entity) + " (" + String.join(", ", names) + ") VALUES ("
				+ String.join(", ", marks) + ")";
		try {
			execute(sql, params);
			log.info("Data: row added to {}", entity);
			return ResponseEntity.ok(Map.of("message", "Saved"));
		} catch (RuntimeException e) {
			return error("Cannot save " + entity, e);
		}
	}

	@GetMapping("/{entity}/{id}")
	public Map<String, Object> findOne(@PathVariable String entity, @PathVariable Long id) {
		log.info("Data: read {} {}", entity, id);
		List<Object> params = new ArrayList<>();
		params.add(id);
		List<Map<String, Object>> rows = select("SELECT * FROM " + tableNameOf(entity) + " WHERE ID = ?", params);
		if (rows.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, entity + " " + id + " not found");
		}
		return rows.get(0);
	}

	@PutMapping("/{entity}/{id}")
	public ResponseEntity<Map<String, String>> update(@PathVariable String entity, @PathVariable Long id,
			@RequestBody Map<String, Object> values) {
		log.info("Data: update {} {} with columns {}", entity, id, values.keySet());
		if (entity.equals("Workplace")) {
			String taken = workplaceUsingIp(values.get("IP"), id);
			if (taken != null) {
				return ResponseEntity.badRequest().body(
						Map.of("message", "IP " + values.get("IP") + " is already assigned to workplace " + taken));
			}
		}
		List<String> sets = new ArrayList<>();
		List<Object> params = new ArrayList<>();

		for (Map<String, Object> column : columns(entity)) {
			String name = (String) column.get("COLUMN_NAME");
			if (values.containsKey(name)) {
				sets.add(name + " = ?");
				params.add(values.get(name));
			}
		}
		params.add(id);

		String sql = "UPDATE " + tableNameOf(entity) + " SET " + String.join(", ", sets) + " WHERE ID = ?";
		try {
			execute(sql, params);
			log.info("Data: {} {} updated", entity, id);
			return ResponseEntity.ok(Map.of("message", "Saved"));
		} catch (RuntimeException e) {
			return error("Cannot save " + entity + " " + id, e);
		}
	}

	// Deletes one row, e.g. DELETE /api/data/Article/5
	@DeleteMapping("/{entity}/{id}")
	public ResponseEntity<Map<String, String>> delete(@PathVariable String entity, @PathVariable Long id) {
		log.info("Data: delete {} {}", entity, id);
		String table = tableNameOf(entity);
		try {
			if (entity.equals("LoadUnit")) {
				String refusal = deleteLoadUnit(id);
				if (refusal != null) {
					log.warn("Data: {}", refusal);
					return ResponseEntity.badRequest().body(Map.of("message", refusal));
				}
			} else {
				List<Object> params = new ArrayList<>();
				params.add(id);
				execute("DELETE FROM " + table + " WHERE ID = ?", params);
			}
			log.info("Data: {} {} deleted", entity, id);
			return ResponseEntity.ok(Map.of("message", "Deleted"));
		} catch (RuntimeException e) {
			return error("Cannot delete " + entity + " " + id, e);
		}
	}

	// A load unit is deleted together with its stock items and reservations, in
	// one transaction. Transport orders block the delete.
	// Returns the refusal message, or null when the load unit was deleted.
	private String deleteLoadUnit(Long id) {
		return new WithSessionAndTransaction<String>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				Number transports = (Number) session.createNamedQuery("TransportOrder.countByLoadUnitId")
						.setParameter("loadUnitId", id).getSingleResult();
				if (transports.intValue() > 0) {
					setReturnValue("Cannot delete load unit " + id + ": it is still used in TRANSPORT_ORDER ("
							+ transports.intValue() + " rows)");
					return;
				}
				session.createNamedMutationQuery("releaseReservationsForLoadUnit").setParameter("loadUnitId", id)
						.executeUpdate();
				session.createNamedMutationQuery("Reservation.deleteByLoadUnitId").setParameter("loadUnitId", id)
						.executeUpdate();
				session.createNamedMutationQuery("StockItem.deleteByLoadUnitId").setParameter("loadUnitId", id)
						.executeUpdate();
				session.createNamedMutationQuery("InvetoryOrderLine.deleteByLoadUnitId").setParameter("loadUnitId", id)
						.executeUpdate();
				session.createNamedMutationQuery("LoadUnit.deleteById").setParameter("id", id).executeUpdate();
			}
		}.run();
	}

	private String workplaceUsingIp(Object ip, Long id) {
		if (ip == null) {
			return null;
		}
		List<Object> params = new ArrayList<>();
		params.add(ip.toString().trim());
		String sql = "SELECT NAME FROM " + tableNameOf("Workplace") + " WHERE UPPER(TRIM(IP)) = UPPER(?)";
		if (id != null) {
			sql = sql + " AND ID <> ?";
			params.add(id);
		}
		List<Map<String, Object>> rows = select(sql, params);
		return rows.isEmpty() ? null : (String) rows.get(0).get("NAME");
	}

	// Runs a SELECT and gives every row as column name -> value (column names upper
	// case, as Oracle sends them)
	private List<Map<String, Object>> select(String sql, List<Object> params) {
		return new WithSessionAndTransaction<List<Map<String, Object>>>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				NativeQuery<Tuple> query = session.createNativeQuery(sql, Tuple.class);
				for (int i = 0; i < params.size(); i++) {
					query.setParameter(i + 1, params.get(i));
				}

				List<Map<String, Object>> rows = new ArrayList<>();
				for (Tuple tuple : query.getResultList()) {
					Map<String, Object> row = new LinkedHashMap<>();
					for (TupleElement<?> element : tuple.getElements()) {
						row.put(element.getAlias(), tuple.get(element));
					}
					rows.add(row);
				}
				setReturnValue(rows);
			}
		}.run();
	}

	// Runs an INSERT, UPDATE or DELETE
	private void execute(String sql, List<Object> params) {
		new WithSessionAndTransaction<Void>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				MutationQuery query = session.createNativeMutationQuery(sql);
				for (int i = 0; i < params.size(); i++) {
					query.setParameter(i + 1, params.get(i));
				}
				query.executeUpdate();
			}
		}.run();
	}

	// A database error: write it in the Eclipse console and send it to Angular
	// (the Oracle message, e.g. ORA-02292)
	private ResponseEntity<Map<String, String>> error(String text, RuntimeException e) {
		Throwable cause = e;
		while (cause.getCause() != null) {
			cause = cause.getCause();
		}
		String message = text + ": " + cause.getMessage();
		log.error(message);
		return ResponseEntity.badRequest().body(Map.of("message", message));
	}

	// Reads one of our dialog JSON files, e.g. "StockItem.json"
	private JsonNode readJson(String file) throws IOException {
		return new ObjectMapper().readTree(new ClassPathResource("static/api/dialogs/" + file).getInputStream());
	}

	// Asks Hibernate which table belongs to an entity class, e.g. "ReceivingOrder"
	// -> "receiving_order"
	private String tableNameOf(String entity) {
		// never the login accounts: they hold the passwords
		if (entity.equals("Account")) {
			log.warn("Data: access to Account refused");
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown entity: " + entity);
		}

		SessionFactoryImplementor sessionFactory = SessionFactoryProvider.getSessionFactory()
				.unwrap(SessionFactoryImplementor.class);
		for (EntityType<?> type : sessionFactory.getMetamodel().getEntities()) {
			if (type.getName().equals(entity)) {
				return sessionFactory.getMappingMetamodel().getEntityDescriptor(type.getJavaType())
						.getIdentifierTableName();
			}
		}
		log.warn("Data: entity {} not found", entity);
		throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown entity: " + entity);
	}
}
