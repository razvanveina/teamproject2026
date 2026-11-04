package ro.demo.usersbackend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.demo.usersbackend.entity.StorageLocation;

public interface StorageLocationRepository extends JpaRepository<StorageLocation, Long> {

//	@Query("SELECT location FROM StorageLocation location WHERE location.loadUnit IS NULL ORDER BY location.x")
//	StorageLocation findFirstEmptyOrderByX();

	Optional<StorageLocation> findFirstByLoadUnitIsNullOrderByXAsc();
}
