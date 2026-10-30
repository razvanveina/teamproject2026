package ro.demo.usersbackend.controller;

import java.util.List;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;

@RestController
@RequestMapping("/api/data")
public class DataController {

	private final JdbcTemplate jdbcTemplate;
	private final EntityManager entityManager;

	public DataController(JdbcTemplate jdbcTemplate, EntityManager entityManager) {
		this.jdbcTemplate = jdbcTemplate;
		this.entityManager = entityManager;
	}

	
	@GetMapping("/{entity}")
	public List<Map<String, Object>> findAll(@PathVariable String entity) {
		// Never send the login accounts: they hold the passwords
		if (entity.equals("Account")) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown entity: " + entity);
		}

		String tableName = tableNameOf(entity);
		return jdbcTemplate.queryForList("SELECT * FROM " + tableName);
	}

	// Asks Hibernate which table belongs to an entity class, e.g. "ReceivingOrder" -> "receiving_order"
	private String tableNameOf(String entity) {
		// Go through all the entity classes Hibernate knows
		for (EntityType<?> type : entityManager.getMetamodel().getEntities()) {
			if (type.getName().equals(entity)) {
				SessionFactoryImplementor sessionFactory = entityManager
						.getEntityManagerFactory()
						.unwrap(SessionFactoryImplementor.class);

				EntityPersister persister = sessionFactory
						.getMappingMetamodel()
						.getEntityDescriptor(type.getJavaType());

				return persister.getIdentifierTableName();
			}
		}

		// No entity class with this name
		throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown entity: " + entity);
	}
}