package ro.demo.usersbackend.controller;

import java.util.List;

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
import ro.demo.usersbackend.dto.PackageOption;
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
		log.info("Receiving: scaned order barcode {}", orderId);

		try {
			return receivingService.findByOrderId(orderId);
		} catch (ResponseStatusException e) {
			log.warn("Receiving: scan error: {}", e.getMessage());
			throw e;
		} catch (Exception e) {
			log.error("Receiving: exception: " + e.getMessage());
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@PutMapping("/finish")
	public ReceivingOrderResponse finishOrder(@RequestBody FinishReceivingRequest request) {
		log.info("Receiving: finished order: {}", request);

		try {
			return receivingService.updateOrder(request);
		} catch (ResponseStatusException e) {
			log.error("Receiving: error occured while finishing order: {}", e.getMessage());
			throw e;
		} catch (Exception e) {
			log.error("Receiving: exception: " + e.getMessage());
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@GetMapping("/package/{barcode}")
	public PackageResponse findPackage(@PathVariable String barcode) {
		log.info("Receiving: scanned package with barcode: {}", barcode);
		try {
			return receivingService.findPackage(barcode);
		} catch (ResponseStatusException e) {
			log.warn("Receiving: error at scanning package barcode: {}", e.getMessage());
			throw e;
		} catch (Exception e) {
			log.error("Receiving: exception: " + e.getMessage());
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}
	
	@GetMapping("/{orderId}/packages")
	public List<PackageOption> findPackageOptions(@PathVariable String orderId){
		log.info("Receiving: packaging options requested for order {}", orderId);
		try {
			return receivingService.findPackageOptions(orderId);
		} catch (ResponseStatusException e) {
			log.warn("Receiving: error at loading packaging options {}", e.getMessage());
			throw e;
		} catch (Exception e) {
			log.error("Receiving: exception {}", e.getMessage());
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}
}
