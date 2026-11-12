package ro.demo.usersbackend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.demo.usersbackend.entity.InventoryOrderLineItem;

public interface InventoryOrderLineItemRepository extends JpaRepository<InventoryOrderLineItem, Long> {
	List<InventoryOrderLineItem> findByInventoryOrderLine_IdOrderByIdAsc(Long inventoryOrderLineId);

	void deleteByInventoryOrderLine_Id(Long inventoryOrderLineId);
}
