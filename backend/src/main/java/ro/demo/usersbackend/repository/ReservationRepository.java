package ro.demo.usersbackend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.demo.usersbackend.entity.Reservation;
import ro.demo.usersbackend.entity.StockItem;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
	List<Reservation> findByStockItemIn(List<StockItem> stockItems);
}
