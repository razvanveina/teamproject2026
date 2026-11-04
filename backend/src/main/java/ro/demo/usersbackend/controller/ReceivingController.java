package ro.demo.usersbackend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ro.demo.usersbackend.dto.ReceivingOrderResponse;
import ro.demo.usersbackend.service.ReceivingLineService;
import ro.demo.usersbackend.service.ReceivingService;

@RestController
@RequestMapping("/api/receiving")
public class ReceivingController {

	private final ReceivingService receivingService;
	private final ReceivingLineService receivingLineService;

	public ReceivingController(ReceivingService receivingService, ReceivingLineService receivingLineService) {
		super();
		this.receivingService = receivingService;
		this.receivingLineService = receivingLineService;
	}

	@GetMapping("/{orderId}")
	public ReceivingOrderResponse findByOrderId(@PathVariable String orderId) {
		return receivingService.findByOrderId(orderId);

		// TODO check if order finished maybe on frontend
	}

	// TODO possibly unnecessary anymore
	@PutMapping("/{orderId}/{articleId}/{quantity}")
	public ReceivingOrderResponse addArticle(@PathVariable String orderId, @PathVariable String articleId,
			@PathVariable int quantity) {

		return receivingLineService.updateArticleQuantity(orderId, articleId, quantity);
	}

	@PutMapping("/finish")
	public ReceivingOrderResponse finishOrder(@RequestBody ReceivingOrderResponse order) {
		return receivingService.updateOrder(order);
	}
}
