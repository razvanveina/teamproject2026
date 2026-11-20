package ro.demo.usersbackend.repository;

import java.util.List;

import org.hibernate.Session;

import ro.demo.usersbackend.entity.InventoryOrderLineItem;

public class InventoryOrderLineItemRepository extends BaseRepository<InventoryOrderLineItem, Long> {
	public InventoryOrderLineItemRepository(Session session) {
		super(session, InventoryOrderLineItem.class);
	}

	public List<InventoryOrderLineItem> findByInventoryOrderLineId(Long inventoryOrderLineId) {
		return session.createNamedQuery(InventoryOrderLineItem.BY_INVENTORY_ORDER_LINE, InventoryOrderLineItem.class)
				.setParameter("inventoryOrderLineId", inventoryOrderLineId).getResultList();
	}

	public void deleteByInventoryOrderLineId(Long inventoryOrderLineId) {
		findByInventoryOrderLineId(inventoryOrderLineId).forEach(this::delete);
	}
}
