package ro.demo.usersbackend.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.demo.usersbackend.dto.ReceivingOrderLineResponse;
import ro.demo.usersbackend.entity.ReceivingOrderLine;
import ro.demo.usersbackend.repository.ReceivingLineRepository;

@Service
@Transactional
public class ReceivingLineService {

	private final ReceivingLineRepository receivingLineRepository;
	private final LoadUnitService loadUnitSerivce;
	private final TransportOrderService transportOrderService;
	private final StorageLocationService storageLocationService;

	public ReceivingLineService(ReceivingLineRepository receivingLineRepository, LoadUnitService loadUnitSerivce,
			TransportOrderService transportOrderService, StorageLocationService storageLocationService) {
		super();
		this.receivingLineRepository = receivingLineRepository;
		this.loadUnitSerivce = loadUnitSerivce;
		this.transportOrderService = transportOrderService;
		this.storageLocationService = storageLocationService;
	}

//	public ReceivingOrderResponse updateArticleQuantity(String orderId, String articleBarcode, int quantity) {
//		ReceivingOrderLine line = receivingLineRepository.findByOrderIdAndArticleBarcode(orderId, articleBarcode)
//				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
//						"Cannot find a receiving order line form the order: " + orderId + " with the article barcode: "
//								+ articleBarcode));
//
////		if(quantity < 0) {
////			throw new ResponseStatusException(HttpStatus)
////		} TODO
//
//		int sumQuantity = line.getReceivedQuantity() + quantity;
//		int maxQuantity = line.getQuantity();
//		if (sumQuantity > maxQuantity) {
//			throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE,
//					"Cannot add more than " + (maxQuantity - line.getReceivedQuantity()) + " articles.");
//		}
//
//		line.setReceivedQuantity(sumQuantity);
//
//		if (line.getQuantity() == line.getReceivedQuantity()) {
//			line.setStatus(ReceivingOrderLineStatus.FINISHED);
//		}
//
//		ReceivingOrder order = line.getOrder();
//
//		if (order.getLines().stream().allMatch(x -> x.getStatus() == ReceivingOrderLineStatus.FINISHED)) {
//			order.setStatus(ReceivingOrderStatus.FINISHED);
//		}
//
//		if (order.getStatus() == ReceivingOrderStatus.FINISHED) {
//			LoadUnit loadUnit = loadUnitSerivce.createLoadUnit(order);
//			Location target = storageLocationService.findClosestEmptyX();
//			transportOrderService.createTransportOrder(loadUnit, target);
//		}
//
//		return new ReceivingOrderResponse(order.getOrderId(), order.getStatus().toString(),
//				getResponseLines(order.getLines()));
//	}

	private List<ReceivingOrderLineResponse> getResponseLines(List<ReceivingOrderLine> lines) {

		List<ReceivingOrderLineResponse> responseLines = new ArrayList<>();
		for (ReceivingOrderLine line : lines) {
			responseLines.add(
					new ReceivingOrderLineResponse(line.getId(), line.getArticle().getDescription(), line.getQuantity(),
							line.getReceivedQuantity(), line.getArticle().getBarcode(), line.getStatus().toString()));
		}

		return responseLines;
	}
}