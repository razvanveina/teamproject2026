package ro.demo.usersbackend.repository;

import java.util.Optional;

import org.hibernate.Session;

import ro.demo.usersbackend.entity.Area;

public class AreaRepository extends BaseRepository<Area, Long> {

	public AreaRepository(Session session) {
		super(session, Area.class);
	}

	public Optional<Area> findByName(String name) {
		return session.createNamedQuery(Area.BY_NAME, Area.class).setParameter("name", name).uniqueResultOptional();
	}

}
