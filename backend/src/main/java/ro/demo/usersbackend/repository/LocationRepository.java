package ro.demo.usersbackend.repository;

import java.util.Optional;

import org.hibernate.Session;

import ro.demo.usersbackend.entity.Location;

public class LocationRepository extends BaseRepository<Location, Integer> {

	public LocationRepository(Session session) {
		super(session, Location.class);
	}

	public Optional<Location> findByName(String name) {
		return session.createNamedQuery(Location.BY_NAME, Location.class).setParameter("name", name)
				.uniqueResultOptional();
	}
}
