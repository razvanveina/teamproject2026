package ro.demo.usersbackend.controller;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ro.demo.usersbackend.dto.PickingOrderResponse;
import ro.demo.usersbackend.dto.PickingRequest;
import ro.demo.usersbackend.service.PickingService;

// adresele statiei de picking, logica e in PickingService
@RestController
@RequestMapping("/api/picking")
public class PickingController {
	public static final Logger log = LogManager.getLogger("application");

	private final PickingService pickingService;

	public PickingController(PickingService pickingService) {
		this.pickingService = pickingService;
	}

	@GetMapping("/next-order")
	public PickingOrderResponse findNextOrder() {
		log.info("Picking: asked for next order");
		return pickingService.findNextOrder();
	}

	@GetMapping("/order/{orderId}")
	public PickingOrderResponse findOrderDetails(@PathVariable String orderId) {
		log.info("Picking: opened order {}", orderId);
		return pickingService.findOrderDetails(orderId);
	}

	@PostMapping("/create-box")
	public ResponseEntity<Map<String, String>> createBox(@RequestBody PickingRequest request) {
		log.info("Picking: created box {} for order {}", request.box(), request.orderId());
		try {
			return ResponseEntity.ok(Map.of("message", pickingService.createBox(request.orderId(), request.box())));
		} catch (ResponseStatusException e) {
			return error(e);
		}
	}

	@PostMapping("/move-to-box")
	public void moveItemToBox(@RequestBody PickingRequest request) {
		log.info("Picking: moved {} to box {} for order {}", request.barcode(), request.box(), request.orderId());
		pickingService.moveItemToBox(request.orderId(), request.barcode(), request.box());
	}

	@PostMapping("/close-box")
	public ResponseEntity<Map<String, String>> closeBox(@RequestBody PickingRequest request) {
		log.info("Picking: closed box {} for order {}", request.box(), request.orderId());
		try {
			return ResponseEntity.ok(Map.of("message", pickingService.closeBox(request.orderId(), request.box())));
		} catch (ResponseStatusException e) {
			return error(e);
		}
	}

	// greselile pe care le vede doar frontendul (articol gresit, cutie gresita) ajung aici ca sa intre in log ca ERROR
	@PostMapping("/log-error")
	public void logError(@RequestBody Map<String, String> body) {
		log.error("Picking: {}", body.get("message"));
	}

	// mesajul erorii ajunge la frontend in "message" si intra in log ca ERROR
	private ResponseEntity<Map<String, String>> error(ResponseStatusException e) {
		log.error("Picking: {}", e.getReason());
		return ResponseEntity.badRequest().body(Map.of("message", e.getReason()));
	}
}
