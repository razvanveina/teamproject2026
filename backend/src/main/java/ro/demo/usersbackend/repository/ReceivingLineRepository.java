package ro.demo.usersbackend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ro.demo.usersbackend.entity.ReceivingOrderLine;

public interface ReceivingLineRepository extends JpaRepository<ReceivingOrderLine, Long> {

	@Query("SELECT line FROM ReceivingOrderLine line WHERE line.order.orderId = :orderId AND line.article.barcode = :articleBarcode")
	Optional<ReceivingOrderLine> findByOrderIdAndArticleBarcode(String orderId, String articleBarcode);

}
