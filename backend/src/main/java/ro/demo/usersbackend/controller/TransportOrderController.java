package ro.demo.usersbackend.controller;

import static ro.demo.usersbackend.logging.ActionLog.LOG;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import ro.demo.usersbackend.dto.TransportOrderErrorRequest;
import ro.demo.usersbackend.dto.TransportOrderRequest;
import ro.demo.usersbackend.dto.TransportOrderResponse;
import ro.demo.usersbackend.service.TransportOrderService;

@RestController
@RequestMapping("/api/transport-orders")
@CrossOrigin(origins = "http://localhost:4200")
public class TransportOrderController {
	public static final Logger log = LogManager.getLogger("application");

	private final TransportOrderService transportOrderService;

	public TransportOrderController(TransportOrderService transportOrderService) {
		this.transportOrderService = transportOrderService;
	}

	@GetMapping("/open")
	public List<TransportOrderResponse> findOpenOrders() {
	    return transportOrderService.findOpenOrders();
	}
	
	@GetMapping("/next")
	public ResponseEntity<TransportOrderResponse> findNextOrder() {
		return transportOrderService.findNextOrder().map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.noContent().build());
	}

	@PostMapping("/{id}/scan")
	public TransportOrderResponse scan(@PathVariable Long id, @Valid @RequestBody TransportOrderRequest request) {
		log.info("Order {}: user scanned '{}'", id, request.barcode());
		try {
			TransportOrderResponse response = transportOrderService.scan(id, request.barcode());
			log.info("Order {}: scan accepted, status is now {}", id, response.status());
			return response;
		} catch (ResponseStatusException e) {
			log.warn("Order {}: scan rejected - {}", id, e.getReason());
			throw e;
		}
	}

	@PostMapping("/{id}/error")
	public TransportOrderResponse reportError(@PathVariable Long id,
			@Valid @RequestBody TransportOrderErrorRequest request) {
		log.info("Order {}: user reported error {}", id, request.reason());
		try {
			TransportOrderResponse response = transportOrderService.reportError(id, request.reason());
			log.info("Order {}: status is now {}", id, response.status());
			return response;
		} catch (ResponseStatusException e) {
			log.warn("Order {}: error report rejected - {}", id, e.getReason());
			throw e;
		}
	}
}
