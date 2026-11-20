package ro.demo.usersbackend.repository;

import java.util.List;
import java.util.Optional;

import org.hibernate.Session;
import org.hibernate.query.spi.Limit;

import ro.demo.usersbackend.entity.StorageLocation;
import ro.demo.usersbackend.entity.TransportOrderStatus;

public class StorageLocationRepository extends BaseRepository<StorageLocation, Integer> {

	public StorageLocationRepository(Session session) {
		super(session, StorageLocation.class);
	}

	public Optional<StorageLocation> findClosestFreeSlot(List<TransportOrderStatus> open, Limit limit) {
		return session.createNamedQuery(StorageLocation.FIND_CLOSEST_FREE_SLOT, StorageLocation.class)
				.uniqueResultOptional();
	}
}