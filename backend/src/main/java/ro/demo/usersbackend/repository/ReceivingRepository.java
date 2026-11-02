package ro.demo.usersbackend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.demo.usersbackend.entity.ReceivingOrder;

public interface ReceivingRepository extends JpaRepository<ReceivingOrder, Long> {

	Optional<ReceivingOrder> findByOrderId(String orderId);

}
