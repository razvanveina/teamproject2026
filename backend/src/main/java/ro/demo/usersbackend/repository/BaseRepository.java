package ro.demo.usersbackend.repository;

import java.util.List;
import java.util.Optional;

import org.hibernate.Session;

public abstract class BaseRepository<T, ID> {

	protected final Session session;
	private final Class<T> entityClass;

	protected BaseRepository(Session session, Class<T> entityClass) {
		this.session = session;
		this.entityClass = entityClass;
	}

	public Optional<T> findById(ID id) {
		return Optional.ofNullable(session.find(entityClass, id));
	}

	public List<T> findAll() {
		return session.createQuery("from " + entityClass.getSimpleName(), entityClass).getResultList();
	}

	public long count() {
		return session.createQuery("select count(e) from " + entityClass.getSimpleName() + " e", Long.class)
				.getSingleResult();
	}

	public boolean existsById(ID id) {
		return findById(id).isPresent();
	}

	public T save(T entity) {
		return session.merge(entity);
	}

	public void insert(T entity) {
		session.persist(entity);
	}

	public void delete(T entity) {
		T managedEntity = entity;

		if (!session.contains(entity)) {
			managedEntity = session.merge(entity);
		}

		session.remove(managedEntity);
	}

	public void deleteById(ID id) {
		T entity = session.find(entityClass, id);

		if (entity != null) {
			session.remove(entity);
		}
	}
}
