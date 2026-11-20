package ro.demo.usersbackend.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.repository.StorageLocationRepository;

@Service
public class StorageLocationService {
	private final StorageLocationRepository storageLocationRepository;

	public StorageLocationService(StorageLocationRepository storageLocationRepository) {
		super();
		this.storageLocationRepository = storageLocationRepository;
	}

	public Location findClosestEmptyX() {
		return storageLocationRepository.findClosestFreeSlot(Constants.OPEN_TRANSPORT_STATUSES).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No free storage location available"));
	}
}
