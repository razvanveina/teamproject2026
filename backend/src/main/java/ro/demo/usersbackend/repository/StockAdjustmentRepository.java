package ro.demo.usersbackend.repository;

import java.util.List;

import org.hibernate.Session;

import ro.demo.usersbackend.entity.StockAdjustment;

public class StockAdjustmentRepository extends BaseRepository<StockAdjustment, Long> {

	public StockAdjustmentRepository(Session session) {
		super(session, StockAdjustment.class);
	}

	public List<StockAdjustment> findByInventoryOrderLineId(Long inventoryOrderLineId) {
		return session.createNamedQuery(StockAdjustment.BY_INVENTORY_ORDER_LINE, StockAdjustment.class)
				.setParameter("inventoryOrderLineId", inventoryOrderLineId).getResultList();
	}
}
