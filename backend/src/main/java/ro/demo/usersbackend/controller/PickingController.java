package ro.demo.usersbackend.controller;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ro.demo.usersbackend.dto.PickingErrorRequest;
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

	// authentication.getName() = userul logat, ca sa nu vada orderele luate de altii
	@GetMapping("/next-order")
	public PickingOrderResponse findNextOrder(Authentication authentication) {
		log.info("Picking: {} asked for next order", authentication.getName());
		return pickingService.findNextOrder(authentication.getName());
	}

	@GetMapping("/orders")
	public List<PickingOrderResponse> findReadyOrders(Authentication authentication) {
		log.info("Picking: {} asked for the list of ready orders", authentication.getName());
		return pickingService.findReadyOrders(authentication.getName());
	}

	@GetMapping("/order/{orderId}")
	public PickingOrderResponse findOrderDetails(@PathVariable String orderId) {
		log.info("Picking: opened order {}", orderId);
		return pickingService.findOrderDetails(orderId);
	}

	@PostMapping("/create-box")
	public ResponseEntity<Map<String, String>> createBox(@RequestBody PickingRequest request, Authentication authentication) {
		log.info("Picking: {} created box {} for order {}", authentication.getName(), request.box(), request.orderId());
		try {
			return ResponseEntity.ok(Map.of("message", pickingService.createBox(request.orderId(), request.box(), authentication.getName())));
		} catch (ResponseStatusException e) {
			return error(e);
		}
	}

	@PostMapping("/take")
	public ResponseEntity<Map<String, String>> takeOrder(@RequestBody PickingRequest request, Authentication authentication) {
		log.info("Picking: {} chose order {}", authentication.getName(), request.orderId());
		try {
			pickingService.takeOrder(request.orderId(), authentication.getName());
			return ResponseEntity.ok(Map.of("message", "Order " + request.orderId() + " taken"));
		} catch (ResponseStatusException e) {
			return error(e);
		}
	}

	@PostMapping("/release")
	public void releaseOrders(Authentication authentication) {
		log.info("Picking: {} left the picking screen", authentication.getName());
		pickingService.releaseOrders(authentication.getName());
	}

	@PostMapping("/move-to-box")
	public void moveItemToBox(@RequestBody PickingRequest request) {
		log.info("Picking: moved stock item {} to box {} for order {}", request.stockItemId(), request.box(), request.orderId());
		pickingService.moveItemToBox(request.stockItemId(), request.box());
	}

	@PostMapping("/error")
	public void reportError(@RequestBody PickingErrorRequest request) {
		log.info("Picking: error {} for stock item {} in order {}, found {}", request.reason(), request.stockItemId(), request.orderId(), request.found());
		pickingService.reportError(request.stockItemId(), request.box(), request.reason(), request.found());
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
