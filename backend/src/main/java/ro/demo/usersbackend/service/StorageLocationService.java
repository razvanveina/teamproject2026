package ro.demo.usersbackend.service;

import org.springframework.stereotype.Service;

import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.repository.StorageLocationRepository;

@Service
public class StorageLocationService {
	private final StorageLocationRepository storageLocationRepository;

	public StorageLocationService(StorageLocationRepository storageLocationRepository) {
		super();
		this.storageLocationRepository = storageLocationRepository;
	}

	public Location findClosestX() {
		return storageLocationRepository.findFirstByOrderByXAsc();
	}
}
