package ro.demo.usersbackend.repository;

import java.util.List;

import org.hibernate.Session;

import ro.demo.usersbackend.entity.Reservation;
import ro.demo.usersbackend.entity.StockItem;

public class ReservationRepository extends BaseRepository<Reservation, Long> {

	public ReservationRepository(Session session) {
		super(session, Reservation.class);
	}

	public List<Reservation> findByStockItemIn(List<StockItem> stockItems) {
		return session.createNamedQuery(Reservation.BY_STOCK_ITEMS, Reservation.class)
				.setParameterList("stockItems", stockItems).getResultList();
	}
}