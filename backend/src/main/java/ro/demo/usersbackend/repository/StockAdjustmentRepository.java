package ro.demo.usersbackend.repository;

import org.hibernate.Session;

import ro.demo.usersbackend.entity.StockAdjustment;

public class StockAdjustmentRepository extends BaseRepository<StockAdjustment, Long> {

	public StockAdjustmentRepository(Session session) {
		super(session, StockAdjustment.class);
	}
}
