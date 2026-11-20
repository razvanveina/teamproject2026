package ro.demo.usersbackend.repository;

import java.util.List;
import java.util.Optional;

import org.hibernate.Session;

import ro.demo.usersbackend.entity.RetrievalOrder;
import ro.demo.usersbackend.entity.RetrievalOrderStatus;

public class RetrievalOrderRepository extends BaseRepository<RetrievalOrder, Long> {

	public RetrievalOrderRepository(Session session) {
		super(session, RetrievalOrder.class);
	}

	public Optional<RetrievalOrder> findByOrderId(String orderId) {
		return session.createNamedQuery(RetrievalOrder.BY_ORDER_ID, RetrievalOrder.class).setParameter("id", orderId)
				.uniqueResultOptional();
	}

	public List<RetrievalOrder> findByStatusOrderByIdAsc(RetrievalOrderStatus status) {
		return session.createNamedQuery(RetrievalOrder.BY_STATUS, RetrievalOrder.class).setParameter("status", status)
				.getResultList();
	}

	public List<RetrievalOrder> findByStatusAndAutomaticFalseOrderByIdAsc(RetrievalOrderStatus status) {
		return session.createNamedQuery(RetrievalOrder.MANUAL_BY_STATUS, RetrievalOrder.class)
				.setParameter("status", status).getResultList();
	}

	public List<RetrievalOrder> findByPickedBy(String pickedBy) {
		return session.createNamedQuery(RetrievalOrder.BY_PICKED_BY, RetrievalOrder.class)
				.setParameter("pickedBy", pickedBy).getResultList();
	}
}