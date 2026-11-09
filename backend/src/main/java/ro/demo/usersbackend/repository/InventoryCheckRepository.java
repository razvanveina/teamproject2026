package ro.demo.usersbackend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.demo.usersbackend.entity.InventoryCheck;

public interface InventoryCheckRepository extends JpaRepository<InventoryCheck, Long> {

	List<InventoryCheck> findByLoadUnitBarcodeOrderByScannedAtDesc(String loadUnitBarcode);
}