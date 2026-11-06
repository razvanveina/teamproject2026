package ro.demo.usersbackend.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ro.demo.usersbackend.dto.FinishReceivingRequest;
import ro.demo.usersbackend.dto.PackageResponse;
import ro.demo.usersbackend.dto.ReceivingOrderResponse;
import ro.demo.usersbackend.service.ReceivingLineService;
import ro.demo.usersbackend.service.ReceivingService;

@RestController
@RequestMapping("/api/receiving")
public class ReceivingController {
	public static final Logger log = LogManager.getLogger("application");

	private final ReceivingService receivingService;

	public ReceivingController(ReceivingService receivingService, ReceivingLineService receivingLineService) {
		super();
		this.receivingService = receivingService;
	}

	@GetMapping("/{orderId}")
	public ReceivingOrderResponse findByOrderId(@PathVariable String orderId) {
		log.info("User scaned order barcode {}", orderId);

		try {
			return receivingService.findByOrderId(orderId);
		} catch (ResponseStatusException e) {
			log.warn("Scan error: {}", e.getMessage());
			throw e;
		} catch (Exception e) {
			log.error("Exception: " + e.getMessage());
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@PutMapping("/finish")
	public ReceivingOrderResponse finishOrder(@RequestBody FinishReceivingRequest request) {
		log.info("User finished order: {}", request);

		try {
			return receivingService.updateOrder(request);
		} catch (ResponseStatusException e) {
			log.error("Error occured while finishing order: {}", e.getMessage());
			throw e;
		} catch (Exception e) {
			log.error("Exception: " + e.getMessage());
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@GetMapping("/package/{barcode}")
	public PackageResponse findPackage(@PathVariable String barcode) {
		log.info("User scanned package with barcode: {}", barcode);
		try {
			return receivingService.findPackage(barcode);
		} catch (ResponseStatusException e) {
			log.warn("Error at scanning package barcode: {}", e.getMessage());
			throw e;
		} catch (Exception e) {
			log.error("Exception: " + e.getMessage());
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}
}
