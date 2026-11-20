package ro.demo.usersbackend.repository;

import java.util.List;

import org.hibernate.Session;

import ro.demo.usersbackend.entity.InventoryOrderLine;
import ro.demo.usersbackend.entity.InventoryOrderLineStatus;
import ro.demo.usersbackend.entity.LoadUnit;

public class InventoryOrderLineRepository extends BaseRepository<InventoryOrderLine, Long> {
	public InventoryOrderLineRepository(Session session) {
		super(session, InventoryOrderLine.class);
	}

	public List<InventoryOrderLine> findByInventoryOrderId(Long inventoryOrderId) {
		return session.createNamedQuery(InventoryOrderLine.BY_INVENTORY_ORDER, InventoryOrderLine.class)
				.setParameter("inventoryOrderId", inventoryOrderId).getResultList();
	}

	public boolean existsByInventoryOrderIdAndStatus(Long inventoryOrderId, InventoryOrderLineStatus status) {
		return session.createNamedQuery(InventoryOrderLine.COUNT_BY_INVENTORY_ORDER_AND_STATUS, Long.class)
				.setParameter("inventoryOrderId", inventoryOrderId).setParameter("status", status)
				.getSingleResult() > 0;
	}

	public boolean existsByInventoryOrderIdAndLocationId(Long inventoryOrderId, Integer locationId) {
		return session.createNamedQuery(InventoryOrderLine.COUNT_BY_INVENTORY_ORDER_AND_LOCATION, Long.class)
				.setParameter("inventoryOrderId", inventoryOrderId).setParameter("locationId", locationId)
				.getSingleResult() > 0;
	}

	public List<InventoryOrderLine> findByLoadUnit(LoadUnit loadUnit) {
		return session.createNamedQuery(InventoryOrderLine.BY_LOAD_UNIT, InventoryOrderLine.class)
				.setParameter("loadUnit", loadUnit).getResultList();
	}
}
