package ro.demo.usersbackend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ro.demo.usersbackend.dto.ReceivingOrderResponse;
import ro.demo.usersbackend.entity.ReceivingOrder;
import ro.demo.usersbackend.service.ReceivingService;

@RestController
@RequestMapping("/receiving")
public class ReceivingController {

	private final ReceivingService receivingService;

	public ReceivingController(ReceivingService receivingService) {
		this.receivingService = receivingService;
	}

	@GetMapping("/{orderId}")
	public ReceivingOrderResponse findByOrderId(@PathVariable String orderId) {
		ReceivingOrder order = receivingService.findByOrderId(orderId);
		return new ReceivingOrderResponse(order.getOrderId(), order.getStatus().toString(), order.getLines());
	}

}
