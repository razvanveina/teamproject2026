package ro.demo.usersbackend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.demo.usersbackend.entity.PackagingUnitType;

public interface PackagingUnitTypeRepository extends JpaRepository<PackagingUnitType, Long> {
	Optional<PackagingUnitType> findByName(String barcode);
}
