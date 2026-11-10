package ro.demo.usersbackend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.demo.usersbackend.entity.RetrievalOrder;
import ro.demo.usersbackend.entity.RetrievalOrderStatus;

public interface RetrievalOrderRepository extends JpaRepository<RetrievalOrder, Long> {

	Optional<RetrievalOrder> findByOrderId(String orderId);

	List<RetrievalOrder> findByStatusOrderByIdAsc(RetrievalOrderStatus status);

	List<RetrievalOrder> findByStatusAndAutomaticFalseOrderByIdAsc(RetrievalOrderStatus status);
}
