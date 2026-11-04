package ro.demo.usersbackend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ro.demo.usersbackend.entity.StorageLocation;

public interface StorageLocationRepository extends JpaRepository<StorageLocation, Long> {

//	@Query("""
//			SELECT location
//			FROM StorageLocation location
//			LEFT JOIN TransportOrder t 
//			ON t.target = location
//			WHERE location.loadUnit IS NULL
//			ORDER BY location.x
//			""")
	@Query("""
			SELECT location
			FROM StorageLocation location
			WHERE location.loadUnit IS NULL
			AND location NOT IN (
				SELECT t.targe
				FROM TransportOrder t
			)
			ORDER BY location.x
			""")
	Optional<StorageLocation> findFirstByLoadUnitIsNullOrderByXAsc();
}
