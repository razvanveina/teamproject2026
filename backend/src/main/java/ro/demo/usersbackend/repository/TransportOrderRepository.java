package ro.demo.usersbackend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.demo.usersbackend.entity.TransportOrder;
import ro.demo.usersbackend.entity.TransportOrderStatus;

public interface TransportOrderRepository extends JpaRepository<TransportOrder, Long> {
	Optional<TransportOrder> findFirstByStatusOrderByIdAsc(TransportOrderStatus status);
	List<TransportOrder> findByStatusOrderByIdAsc(TransportOrderStatus status);
}
