package ro.demo.usersbackend.controller;

import java.io.IOException;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ro.demo.usersbackend.service.HostTestingService;

@RestController
@RequestMapping("/api/host-testing")
public class HostTestingController {
	private final Logger log = LogManager.getLogger("application");
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
