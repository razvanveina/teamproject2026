package ro.demo.usersbackend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ro.demo.usersbackend.entity.StorageLocation;
import ro.demo.usersbackend.entity.TransportOrderStatus;

public interface StorageLocationRepository extends JpaRepository<StorageLocation, Integer> {

	@Query("select l from StorageLocation l "
			+ "where not exists (select u from LoadUnit u where u.location = l) "
			+ "and l.aisle > 0 "
			+ "and l.id not in (select t.target.id from TransportOrder t where t.status in ?1) "
			+ "order by l.x asc")
	Optional<StorageLocation> findFirstFreeByX(List<TransportOrderStatus> open, Limit limit);
}