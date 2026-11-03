package ro.demo.usersbackend.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ro.demo.usersbackend.dto.ReceivingOrderLineResponse;
import ro.demo.usersbackend.dto.ReceivingOrderResponse;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.ReceivingOrder;
import ro.demo.usersbackend.entity.ReceivingOrderLine;
import ro.demo.usersbackend.entity.ReceivingOrderStatus;
import ro.demo.usersbackend.service.LoadUnitService;
import ro.demo.usersbackend.service.ReceivingLineService;
import ro.demo.usersbackend.service.ReceivingService;
import ro.demo.usersbackend.service.StorageLocationService;
import ro.demo.usersbackend.service.TransportOrderService;

@RestController
@RequestMapping("/api/receiving")
public class ReceivingController {

	private final ReceivingService receivingService;
	private final ReceivingLineService receivingLineService;
	private final LoadUnitService loadUnitSerivce;
	private final TransportOrderService transportOrderService;
	private final StorageLocationService storageLocationService;

	public ReceivingController(ReceivingService receivingService, ReceivingLineService receivingLineService,
			LoadUnitService loadUnitSerivce, TransportOrderService transportOrderService,
			StorageLocationService storagelocationService) {
		super();
		this.receivingService = receivingService;
		this.receivingLineService = receivingLineService;
		this.loadUnitSerivce = loadUnitSerivce;
		this.transportOrderService = transportOrderService;
		this.storageLocationService = storagelocationService;
	}

	@GetMapping("/{orderId}")
	public ReceivingOrderResponse findByOrderId(@PathVariable String orderId) {
		ReceivingOrder order = receivingService.findByOrderId(orderId);

		return new ReceivingOrderResponse(order.getOrderId(), order.getStatus().toString(),
				getResponseLines(order.getLines()));
	}

	@PutMapping("/{orderId}/{articleId}/{quantity}")
	public ReceivingOrderResponse addArticle(@PathVariable String orderId, @PathVariable String articleId,
			@PathVariable int quantity) {

		ReceivingOrder order = receivingLineService.updateArticleQuantity(orderId, articleId, quantity);

		if (order.getStatus() == ReceivingOrderStatus.FINISHED) {
			LoadUnit loadUnit = loadUnitSerivce.createLoadUnit(order);
			Location target = storageLocationService.findClosestX();
			transportOrderService.createTransportOrder(loadUnit, target);
		}

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
}
