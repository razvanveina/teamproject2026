package ro.demo.usersbackend.repository;

import org.hibernate.Session;

import ro.demo.usersbackend.entity.RetrievalOrderLine;
import ro.demo.usersbackend.entity.RetrievalOrderLineStatus;

public class RetrievalOrderLineRepository extends BaseRepository<RetrievalOrderLine, Long> {

	public RetrievalOrderLineRepository(Session session) {
		super(session, RetrievalOrderLine.class);
	}

	public long countByOrderOrderIdAndStatusNot(String orderId, RetrievalOrderLineStatus status) {
		return session.createNamedQuery(RetrievalOrderLine.COUNT_BY_ORDER_ID_AND_STATUS_NOT, Long.class)
				.setParameter("orderId", orderId).setParameter("status", status).getSingleResult();
	}
}