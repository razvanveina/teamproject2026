package ro.demo.usersbackend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ro.demo.usersbackend.entity.Location;

public interface LocationRepository extends JpaRepository<Location, Integer> {

	@Query(name = "Location.findByName")
	Optional<Location> findByName(String string);

}
