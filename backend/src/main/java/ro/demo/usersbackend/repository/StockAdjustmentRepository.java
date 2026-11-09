package ro.demo.usersbackend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.demo.usersbackend.entity.StockAdjustment;

public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, Long> {

	List<StockAdjustment> findByInventoryCheck_IdOrderByIdAsc(Long inventoryCheckId);
}