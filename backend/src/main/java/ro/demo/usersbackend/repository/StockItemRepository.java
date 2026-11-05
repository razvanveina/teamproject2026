package ro.demo.usersbackend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.StockItem;

public interface StockItemRepository extends JpaRepository<StockItem, Long> {
	List<StockItem> findByLoadUnitAndReservationIsNullOrderByIdAsc(LoadUnit loadUnit);
}

