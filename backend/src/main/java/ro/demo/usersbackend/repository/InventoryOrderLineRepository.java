package ro.demo.usersbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.demo.usersbackend.entity.InventoryOrderLine;

public interface InventoryOrderLineRepository extends JpaRepository<InventoryOrderLine, Long> {
}
