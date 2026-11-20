package ro.demo.usersbackend.repository;

import java.util.Optional;

import org.hibernate.Session;

import ro.demo.usersbackend.entity.PackagingUnitType;

public class PackagingUnitTypeRepository extends BaseRepository<PackagingUnitType, Long> {

	public PackagingUnitTypeRepository(Session session) {
		super(session, PackagingUnitType.class);
	}

	public Optional<PackagingUnitType> findByName(String name) {
		return session.createNamedQuery(PackagingUnitType.BY_NAME, PackagingUnitType.class).setParameter("name", name)
				.uniqueResultOptional();
	}
}
