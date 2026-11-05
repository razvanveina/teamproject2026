package ro.demo.usersbackend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.demo.usersbackend.entity.LoadUnit;

public interface LoadUnitRepository extends JpaRepository<LoadUnit, Long> {
	Optional<LoadUnit> findByBarcode(String barcode);
}
