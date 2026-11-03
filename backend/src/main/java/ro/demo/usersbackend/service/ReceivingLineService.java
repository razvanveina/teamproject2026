package ro.demo.usersbackend.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ro.demo.usersbackend.entity.ReceivingOrder;
import ro.demo.usersbackend.entity.ReceivingOrderLine;
import ro.demo.usersbackend.entity.ReceivingOrderLineStatus;
import ro.demo.usersbackend.entity.ReceivingOrderStatus;
import ro.demo.usersbackend.repository.ReceivingLineRepository;

@Service
@Transactional
public class ReceivingLineService {

	private final ReceivingLineRepository receivingLineRepository;

	public ReceivingLineService(ReceivingLineRepository receivingLineRepository) {
		super();
		this.receivingLineRepository = receivingLineRepository;
	}

	public ReceivingOrder updateArticleQuantity(String orderId, String articleBarcode, int quantity) {
		ReceivingOrderLine line = receivingLineRepository.findByOrderIdAndArticleBarcode(orderId, articleBarcode)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Cannot find a receiving order line form the order: " + orderId + " with the article barcode: "
								+ articleBarcode));

//		if(quantity < 0) {
//			throw new ResponseStatusException(HttpStatus)
//		} TODO

		int sumQuantity = line.getReceivedQuantity() + quantity;
		int maxQuantity = line.getQuantity();
		if (sumQuantity > maxQuantity) {
			throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE,
					"Cannot add more than " + (maxQuantity - line.getReceivedQuantity()) + " articles.");
		}

		line.setReceivedQuantity(sumQuantity);

		if (line.getQuantity() == line.getReceivedQuantity()) {
			line.setStatus(ReceivingOrderLineStatus.FINISHED);
		}

		ReceivingOrder order = line.getOrder();

		if (order.getLines().stream().allMatch(x -> x.getStatus() == ReceivingOrderLineStatus.FINISHED)) {
			order.setStatus(ReceivingOrderStatus.FINISHED);
		}

		return order;
	}// TODO add logs
}