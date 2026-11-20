package ro.demo.usersbackend.repository;

import java.util.List;

import org.hibernate.Session;

import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.StockItem;

public class StockItemRepository extends BaseRepository<StockItem, Long> {

	public StockItemRepository(Session session) {
		super(session, StockItem.class);
	}

	public List<StockItem> findByLoadUnitOrderByIdAsc(LoadUnit loadUnit) {
		return session.createNamedQuery(StockItem.FIND_BY_LOADUNIT_ORDER_BY_ID_ASC, StockItem.class)
				.setParameter("loadUnit", loadUnit).getResultList();
	};
}
