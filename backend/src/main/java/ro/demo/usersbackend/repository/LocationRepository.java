package ro.demo.usersbackend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.demo.usersbackend.entity.Location;

public interface LocationRepository extends JpaRepository<Location, Long> {

	Optional<Location> findByName(String string);

}
