package ro.demo.usersbackend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.StorageLocation;

public interface StorageLocationRepository extends JpaRepository<StorageLocation, Long> {

	// inainte era where l.loadUnit is null, insa loc nu mai are fieldul loadunit
	@Query("select l from StorageLocation l " + "where not exists (select u from LoadUnit u where u.location = l) "
			+ "and l.aisle > 0 " + "and l.id not in (select t.target.id from TransportOrder t) " + "order by l.x asc")
	Optional<StorageLocation> findFirstFreeByX(Limit limit);

	@Query("select l from StorageLocation l join LoadUnit u on u.location = l where u = ?1")
	List<StorageLocation> findByLoadUnit(LoadUnit loadUnit);
}