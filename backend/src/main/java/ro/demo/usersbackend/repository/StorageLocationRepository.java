package ro.demo.usersbackend.repository;

import java.util.List;
import java.util.Optional;

import org.hibernate.Session;

import ro.demo.usersbackend.entity.StorageLocation;
import ro.demo.usersbackend.entity.TransportOrderStatus;

public class StorageLocationRepository extends BaseRepository<StorageLocation, Integer> {

	public StorageLocationRepository(Session session) {
		super(session, StorageLocation.class);
	}

	public Optional<StorageLocation> findByName(String name) {
		return session.createNamedQuery(StorageLocation.FIND_BY_NAME, StorageLocation.class).setParameter("name", name)
				.uniqueResultOptional();
	}

	public Optional<StorageLocation> findClosestFreeSlot(List<TransportOrderStatus> open) {
		return session.createNamedQuery(StorageLocation.FIND_CLOSEST_FREE_SLOT, StorageLocation.class)
				.setParameterList(1, open).setMaxResults(1).uniqueResultOptional();
	}
}