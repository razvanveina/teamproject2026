package ro.demo.usersbackend.controller;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;

@RestController
@RequestMapping("/api/data")
public class DataController {

	// Writes messages in the Eclipse console
	private static final Logger log = LoggerFactory.getLogger(DataController.class);

	private final JdbcTemplate jdbcTemplate;
	private final EntityManager entityManager;

	public DataController(JdbcTemplate jdbcTemplate, EntityManager entityManager) {
		this.jdbcTemplate = jdbcTemplate;
		this.entityManager = entityManager;
	}

	// All rows of a table, plus the columns of the linked tables from entitySpec
	// e.g. GET /api/data/StockItem also brings ARTICLE_ID__BARCODE and ARTICLE_ID__DESCRIPTION
	@GetMapping("/{entity}")
	public List<Map<String, Object>> findAll(@PathVariable String entity, @RequestParam Map<String, String> filters) throws IOException {
		// no dialog file (e.g. StorageLocation, used by the home page): just all the rows
		log.info("Data: {} with filters {}", entity, filters);
		if (!new ClassPathResource("static/api/dialogs/" + entity + ".json").exists()) {
			return jdbcTemplate.queryForList("SELECT * FROM " + tableNameOf(entity));
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
								joins = joins + " LEFT JOIN " + tableNameOf(show.get("from").asText()) + " " + j2 + " ON " + j2 + ".ID = " + j + "." + name;
								select = select + ", " + j2 + "." + show.get("column").asText() + " AS " + field + "__" + name;
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
			paging = " ORDER BY " + sortField + " " + direction + tieBreaker
					+ " OFFSET ? ROWS FETCH NEXT 20 ROWS ONLY";
			params.add(Integer.parseInt(filters.get("page")) * 20);
		} else {
			paging = " ORDER BY " + sortField + " " + direction + tieBreaker;
		}

		return jdbcTemplate.queryForList(select + " FROM " + tableNameOf(entity) + " t" + joins + where + paging, params.toArray());
	}

	// The columns of a table: name, type and if it can be empty
	@GetMapping("/{entity}/columns")
	public List<Map<String, Object>> columns(@PathVariable String entity) {
		log.info("Data: columns of table {}", entity);
		String sql = "SELECT COLUMN_NAME, DATA_TYPE, NULLABLE FROM USER_TAB_COLUMNS WHERE TABLE_NAME = ?";
		return jdbcTemplate.queryForList(sql, tableNameOf(entity).toUpperCase());
	}

	// Adds one row, e.g. POST /api/data/Destination with { "NAME": "Sibiu" }
	@PostMapping("/{entity}")
	public ResponseEntity<Map<String, String>> add(@PathVariable String entity, @RequestBody Map<String, Object> values) {
		log.info("Data: added a row to {} with columns {}", entity, values.keySet());
		if ("InventoryOrder".equals(entity) && values.get("CREATED_AT") == null) {
			values.put("CREATED_AT", Timestamp.valueOf(java.time.LocalDateTime.now()));
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
			log.info("Data: row added to {}", entity);
			jdbcTemplate.update(sql, params.toArray());
			return ResponseEntity.ok(Map.of("message", "Saved"));
		} catch (DataAccessException e) {
			return error("Cannot save " + entity, e);
		}
	}

	@GetMapping("/{entity}/{id}")
	public Map<String, Object> findOne(@PathVariable String entity, @PathVariable Long id) {
		log.info("Data: read {} {}", entity, id);
		return jdbcTemplate.queryForMap("SELECT * FROM " + tableNameOf(entity) + " WHERE ID = ?", id);
	}

	@PutMapping("/{entity}/{id}")
	public ResponseEntity<Map<String, String>> update(@PathVariable String entity, @PathVariable Long id, @RequestBody Map<String, Object> values) {
		log.info("Data: update {} {} with columns {}", entity, id, values.keySet());
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
			log.info("Data: {] {} updated", entity, id);
			jdbcTemplate.update(sql, params.toArray());
			return ResponseEntity.ok(Map.of("message", "Saved"));
		} catch (DataAccessException e) {
			return error("Cannot save " + entity + " " + id, e);
		}
	}

	// Deletes one row, e.g. DELETE /api/data/Article/5
	@DeleteMapping("/{entity}/{id}")
	public ResponseEntity<Map<String, String>> delete(@PathVariable String entity, @PathVariable Long id) {
		try {
			log.info("Data: delete {} {} ", entity, id);
			jdbcTemplate.update("DELETE FROM " + tableNameOf(entity) + " WHERE ID = ?", id);
			log.info("Data: {} {} deleted", entity, id);
			return ResponseEntity.ok(Map.of("message", "Deleted"));
		} catch (DataAccessException e) {
			return error("Cannot delete " + entity + " " + id, e);
		}
	}

	// A database error: write it in the Eclipse console and send it to Angular
	private ResponseEntity<Map<String, String>> error(String text, DataAccessException e) {
		String message = text + ": " + e.getMostSpecificCause().getMessage();
		log.error(message);
		return ResponseEntity.badRequest().body(Map.of("message", message));
	}

	// Reads one of our dialog JSON files, e.g. "StockItem.json"
	private JsonNode readJson(String file) throws IOException {
		return new ObjectMapper().readTree(new ClassPathResource("static/api/dialogs/" + file).getInputStream());
	}

	// Asks Hibernate which table belongs to an entity class, e.g. "ReceivingOrder" -> "receiving_order"
	private String tableNameOf(String entity) {
		// never the login accounts: they hold the passwords
		if (entity.equals("Account")) {
			log.warn("Data: access to Account refused");
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown entity: " + entity);
		}

		for (EntityType<?> type : entityManager.getMetamodel().getEntities()) {
			if (type.getName().equals(entity)) {
				SessionFactoryImplementor sessionFactory = entityManager.getEntityManagerFactory()
						.unwrap(SessionFactoryImplementor.class);
				return sessionFactory.getMappingMetamodel().getEntityDescriptor(type.getJavaType())
						.getIdentifierTableName();
			}
		}
		log.warn("Data: entity {} not found", entity);
		throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown entity: " + entity);
	}
}
