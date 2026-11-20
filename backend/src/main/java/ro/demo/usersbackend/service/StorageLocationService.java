package ro.demo.usersbackend.service;

import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.StorageLocation;
import ro.demo.usersbackend.repository.StorageLocationRepository;

@Service
public class StorageLocationService {
	public StorageLocationService() {
		super();
	}

	public Location findClosestEmptyX() {
		return new WithSessionAndTransaction<StorageLocation>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				StorageLocationRepository slr = new StorageLocationRepository(session);
				setReturnValue(slr.findClosestFreeSlot(Constants.OPEN_TRANSPORT_STATUSES)
						.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
								"No free storage location available")));
			}
		}.run();
	}
}
