package ro.demo.usersbackend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ro.demo.usersbackend.entity.LoadUnit;
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
	@Query("select l from StorageLocation l " + "where l.loadUnit is null and l.aisle > 0 "
			+ "and l.id not in (select t.target.id from TransportOrder t) " + "order by l.x asc")
	Optional<StorageLocation> findFirstFreeByX(Limit limit);

	List<StorageLocation> findByLoadUnit(LoadUnit loadUnit);
}
