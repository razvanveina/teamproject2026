package ro.demo.usersbackend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.demo.usersbackend.entity.InventoryOrder;
import ro.demo.usersbackend.entity.InventoryOrderStatus;

public interface InventoryOrderRepository extends JpaRepository<InventoryOrder, Long> {
	List<InventoryOrder> findByStatusOrderByCreatedAtDesc(InventoryOrderStatus status);
}
