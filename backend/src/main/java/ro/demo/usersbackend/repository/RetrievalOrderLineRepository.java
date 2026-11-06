package ro.demo.usersbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.demo.usersbackend.entity.RetrievalOrderLine;
import ro.demo.usersbackend.entity.RetrievalOrderLineStatus;

public interface RetrievalOrderLineRepository extends JpaRepository<RetrievalOrderLine, Long> {

	long countByOrderOrderIdAndStatusNot(String orderId, RetrievalOrderLineStatus status);
}
