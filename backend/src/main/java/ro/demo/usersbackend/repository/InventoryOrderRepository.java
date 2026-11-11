package ro.demo.usersbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.demo.usersbackend.entity.InventoryOrder;

public interface InventoryOrderRepository extends JpaRepository<InventoryOrder, Long> {
}
