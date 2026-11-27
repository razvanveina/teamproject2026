package ro.demo.usersbackend.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ro.demo.usersbackend.dto.OrderCheckResponse;
import ro.demo.usersbackend.service.HostTestingService;

@RestController
@RequestMapping("/api/host-testing")
public class HostTestingController {
	private static final Logger log = LogManager.getLogger(HostTestingController.class);
	private final HostTestingService hostTestingService;

	public HostTestingController(HostTestingService hostTestingService) {
		this.hostTestingService = hostTestingService;
	}

	@PostMapping("/receiving-order")
	public ResponseEntity<Map<String, String>> createReceivingOrder() {
		try {
			return ResponseEntity.ok(Map.of("fileName", hostTestingService.createReceivingOrder()));
		} catch (IOException e) {
			log.error("Host testing: receiving order file not created", e);
			return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
		}
	}

	@GetMapping("/order-check")
	public List<OrderCheckResponse> checkRetrievalOrders() {
		return hostTestingService.checkRetrievalOrders();
	}

	@PostMapping("/retrieval-order")
	public ResponseEntity<Map<String, String>> createRetrievalOrder() {
		try {
			return ResponseEntity.ok(Map.of("fileName", hostTestingService.createRetrievalOrder()));
		} catch (IOException e) {
			log.error("Host testing: retrieval order file not created", e);
			return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
		}
	}
}
