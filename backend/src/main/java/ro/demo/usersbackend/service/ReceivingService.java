package ro.demo.usersbackend.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ro.demo.usersbackend.dto.ReceivingOrderLineResponse;
import ro.demo.usersbackend.dto.ReceivingOrderResponse;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.ReceivingOrder;
import ro.demo.usersbackend.entity.ReceivingOrderLine;
import ro.demo.usersbackend.entity.ReceivingOrderLineStatus;
import ro.demo.usersbackend.entity.ReceivingOrderStatus;
import ro.demo.usersbackend.repository.ReceivingRepository;

@Service
@Transactional
public class ReceivingService {

	private final LoadUnitService loadUnitService;
	private final StorageLocationService storageLocationService;
	private final TransportOrderService transportOrderService;

	private final ReceivingRepository receivingRepository;

	public ReceivingService(LoadUnitService loadUnitService, StorageLocationService storageLocationService,
			TransportOrderService transportOrderService, ReceivingRepository receivingRepository) {
		super();
		this.loadUnitService = loadUnitService;
		this.storageLocationService = storageLocationService;
		this.transportOrderService = transportOrderService;
		this.receivingRepository = receivingRepository;
	}

	@Transactional(readOnly = true)
	public ReceivingOrderResponse findByOrderId(String orderId) {
		ReceivingOrder order = receivingRepository.findByOrderId(orderId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Cannot find receiving order with the id: " + orderId));

		return new ReceivingOrderResponse(order.getOrderId(), order.getStatus().toString(),
				getResponseLines(order.getLines()));
	}

	private List<ReceivingOrderLineResponse> getResponseLines(List<ReceivingOrderLine> lines) {

		List<ReceivingOrderLineResponse> responseLines = new ArrayList<>();
		for (ReceivingOrderLine line : lines) {
			responseLines.add(
					new ReceivingOrderLineResponse(line.getId(), line.getArticle().getDescription(), line.getQuantity(),
							line.getReceivedQuantity(), line.getArticle().getBarcode(), line.getStatus().toString()));
		}

		return responseLines;
	}

	@Transactional
	public ReceivingOrderResponse updateOrder(ReceivingOrderResponse orderResponse) {
		ReceivingOrder order = receivingRepository.findByOrderId(orderResponse.orderId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Cannot find receiving order with the id: " + orderResponse.orderId()));

		order.setStatus(ReceivingOrderStatus.valueOf(orderResponse.status()));

		for (ReceivingOrderLine line : order.getLines()) { // TODO these lines MIGHT be in the same order on frontend
			for (ReceivingOrderLineResponse l : orderResponse.lines()) {
				if (line.getArticle().getBarcode().equals(l.barcode())) {
					line.setReceivedQuantity(l.receivedQuantity());
					line.setStatus(ReceivingOrderLineStatus.valueOf(l.status()));
					break;
				}
			}
		}

		createTransportOrder(order);

		return new ReceivingOrderResponse(order.getOrderId(), order.getStatus().toString(),
				getResponseLines(order.getLines()));
	}

	private void createTransportOrder(ReceivingOrder order) {
		LoadUnit loadUnit = loadUnitService.createLoadUnit(order);
		Location target = storageLocationService.findClosestEmptyX();
		transportOrderService.createTransportOrder(loadUnit, target);

	}
}
