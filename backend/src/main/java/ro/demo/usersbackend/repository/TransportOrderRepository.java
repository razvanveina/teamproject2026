package ro.demo.usersbackend.repository;

import java.util.Collection;
import java.util.List;

import org.hibernate.Session;

import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.TransportOrder;
import ro.demo.usersbackend.entity.TransportOrderStatus;

public class TransportOrderRepository extends BaseRepository<TransportOrder, Long> {
	public TransportOrderRepository(Session session) {
		super(session, TransportOrder.class);
	}

	public List<TransportOrder> findByStatusAndNotSourceOrderByIdAsc(TransportOrderStatus status, Location source) {
		return session.createNamedQuery(TransportOrder.FIND_BY_STATUS_AND_NOT_SOURCE_ODERED_BY_ID, TransportOrder.class)
				.setParameter("status", status).setParameter("source", source).getResultList();
	}

	public List<TransportOrder> findByStatusAndSourceOrderedByIdAsc(TransportOrderStatus status, Location source) {
		return session.createNamedQuery(TransportOrder.FIND_BY_STATUS_AND_SOURCE_ODERED_BY_ID, TransportOrder.class)
				.setParameter("status", status).setParameter("source", source).getResultList();
	}

	public List<TransportOrder> findByStatusAndAssignedToOrderByIdAsc(String assignedTo,
			Collection<TransportOrderStatus> statuses) {
		return session
				.createNamedQuery(TransportOrder.FIND_BY_STATUS_AND_ASSIGNED_TO_ODERED_BY_ID, TransportOrder.class)
				.setParameter("assignedTo", assignedTo).setParameterList("statuses", statuses).getResultList();
	}

	public List<TransportOrder> findByStatusAndAssignedToIsNullOrderByIdAsc(Collection<TransportOrderStatus> statuses) {
		return session.createNamedQuery(TransportOrder.FIND_BY_STATUS_AND_ASSIGNED_TO_IS_NULL_ODERED_BY_ID,
				TransportOrder.class).setParameterList("statuses", statuses).getResultList();
	}

	public List<TransportOrder> findActiveAndNewByLoadUnit(LoadUnit loadUnit) {
		return session.createNamedQuery(TransportOrder.FIND_ACTIVE_AND_NEW_BY_LOAD_UNIT, TransportOrder.class)
				.setParameterList("status", List.of(TransportOrderStatus.NEW, TransportOrderStatus.ACTIVE))
				.setParameter("loadUnit", loadUnit).getResultList();
	}

	public List<TransportOrder> findActiveByLoadUnit(LoadUnit loadUnit) {
		return session.createNamedQuery(TransportOrder.FIND_ACTIVE_BY_LOAD_UNIT, TransportOrder.class)
				.setParameter("status", TransportOrderStatus.ACTIVE).setParameter("loadUnit", loadUnit).getResultList();
	}
}