package ro.demo.usersbackend.service;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
		return storageLocationRepository.findFirstFreeByX(Limit.of(1)).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No free storage location available"));
	}
}
