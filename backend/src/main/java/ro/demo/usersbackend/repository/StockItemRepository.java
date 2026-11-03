package ro.demo.usersbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.demo.usersbackend.entity.StockItem;

public interface StockItemRepository extends JpaRepository<StockItem, Long> {
}
