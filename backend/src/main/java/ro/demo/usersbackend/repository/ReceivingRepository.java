package ro.demo.usersbackend.repository;

import java.util.Optional;

import org.hibernate.Session;

import ro.demo.usersbackend.entity.ReceivingOrder;

public class ReceivingRepository extends BaseRepository<ReceivingOrder, Long> {

	public ReceivingRepository(Session session) {
		super(session, ReceivingOrder.class);
	}

	public Optional<ReceivingOrder> findByOrderId(String orderId) {
		return session.createNamedQuery(ReceivingOrder.BY_ORDER_ID, ReceivingOrder.class)
				.setParameter("orderId", orderId).uniqueResultOptional();
	}
}