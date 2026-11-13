package ro.demo.usersbackend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.Location;

public interface LoadUnitRepository extends JpaRepository<LoadUnit, Long> {
	Optional<LoadUnit> findByBarcode(String barcode);
	List<LoadUnit> findByLocationOrderByIdAsc(Location location);
	List<LoadUnit> findByLocationName(String name);
}
