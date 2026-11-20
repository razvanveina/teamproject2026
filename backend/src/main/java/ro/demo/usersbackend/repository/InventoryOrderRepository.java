package ro.demo.usersbackend.repository;

import java.util.Optional;

import org.hibernate.Session;

import ro.demo.usersbackend.entity.InventoryOrder;
import ro.demo.usersbackend.entity.InventoryOrderStatus;

public class InventoryOrderRepository extends BaseRepository<InventoryOrder, Long> {

	public InventoryOrderRepository(Session session) {
		super(session, InventoryOrder.class);
	}

	public Optional<InventoryOrder> findByCodeAndStatus(String code, InventoryOrderStatus status) {
		return session.createNamedQuery(InventoryOrder.BY_CODE_AND_STATUS, InventoryOrder.class)
				.setParameter("code", code).setParameter("status", status).uniqueResultOptional();
	}

	public boolean existsByCode(String code) {
		return session.createNamedQuery(InventoryOrder.COUNT_BY_CODE, Long.class).setParameter("code", code)
				.getSingleResult() > 0;
	}
}