package ro.demo.usersbackend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ro.demo.usersbackend.dto.TransportOrderRequest;
import ro.demo.usersbackend.dto.TransportOrderResponse;
import ro.demo.usersbackend.service.TransportOrderService;

@RestController
@RequestMapping("/api/transport-orders")
@CrossOrigin(origins = "http://localhost:4200")
public class TransportOrderController {

	private final TransportOrderService transportOrderService;

	public TransportOrderController(TransportOrderService transportOrderService) {
		this.transportOrderService = transportOrderService;
	}

	@GetMapping("/next")
	public ResponseEntity<TransportOrderResponse> findNextOrder() {
		return transportOrderService.findNextOrder().map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.noContent().build());
	}

	@PostMapping("/{id}/scan")
	public TransportOrderResponse scan(@PathVariable Long id, @Valid @RequestBody TransportOrderRequest request) {
		return transportOrderService.scan(id, request.barcode());
	}
}
