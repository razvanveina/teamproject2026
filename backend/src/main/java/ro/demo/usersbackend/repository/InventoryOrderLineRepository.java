package ro.demo.usersbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.demo.usersbackend.entity.InventoryOrderLine;
import ro.demo.usersbackend.entity.InventoryOrderLineStatus;

public interface InventoryOrderLineRepository extends JpaRepository<InventoryOrderLine, Long> {
	java.util.List<InventoryOrderLine> findByInventoryOrder_IdOrderByIdAsc(Long inventoryOrderId);

	boolean existsByInventoryOrder_IdAndStatus(Long inventoryOrderId, InventoryOrderLineStatus status);

	boolean existsByInventoryOrder_IdAndLocation_Id(Long inventoryOrderId, Integer locationId);
}
