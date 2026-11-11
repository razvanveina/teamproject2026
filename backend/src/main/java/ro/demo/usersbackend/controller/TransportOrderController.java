package ro.demo.usersbackend.controller;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

	private static final Object ASSIGN_LOCK = new Object();
	
	private final TransportOrderService transportOrderService;
	
	public TransportOrderController(TransportOrderService transportOrderService) {
		this.transportOrderService = transportOrderService;
	}

	@GetMapping("/open")
	public List<TransportOrderResponse> findOpenOrders(Authentication authentication) {
		log.info("Transport: {} listed the open orders", authentication.getName());
		return transportOrderService.findOpenOrders(authentication.getName());
	}

	@GetMapping("/openTote")
	public List<TransportOrderResponse> findOpenToteOrders(Authentication authentication) {
		log.info("Transport: {} listed the open tote orders", authentication.getName());
		return transportOrderService.findOpenToteOrders(authentication.getName());
	}

	@GetMapping("/next")
	public ResponseEntity<TransportOrderResponse> findNextOrder(Authentication authentication) {
		log.info("Transport: {} asked for next order", authentication.getName());
		synchronized (ASSIGN_LOCK) {
			return transportOrderService.findNextOrder(authentication.getName()).map(ResponseEntity::ok)
					.orElseGet(() -> ResponseEntity.noContent().build());
		}
	}

	@GetMapping("/nextTote")
	public ResponseEntity<TransportOrderResponse> findNextToteTransport(Authentication authentication) {
		log.info("Transport: {} asked for next tote order", authentication.getName());
		synchronized (ASSIGN_LOCK) {
			return transportOrderService.findNextToteOrder(authentication.getName()).map(ResponseEntity::ok)
					.orElseGet(() -> ResponseEntity.noContent().build());
		}
	}

	@PostMapping("/{id}/take")
	public TransportOrderResponse take(@PathVariable Long id, Authentication authentication) {
		log.info("Transport: {} takes order {}", authentication.getName(), id);
		synchronized (ASSIGN_LOCK) {
			return transportOrderService.take(id, authentication.getName());
		}
	}

	@PostMapping("/{id}/release")
	public void release(@PathVariable Long id, Authentication authentication) {
		log.info("Transport: {} releases order {}", authentication.getName(), id);
		synchronized (ASSIGN_LOCK) {
			transportOrderService.release(id, authentication.getName());
		}
	}

	@PostMapping("/{id}/scan")
	public TransportOrderResponse scan(@PathVariable Long id, @Valid @RequestBody TransportOrderRequest request,
			Authentication authentication) {
		log.info("Transport: order {} user {} scanned '{}'", id, authentication.getName(), request.barcode());
		try {
			TransportOrderResponse response;
			synchronized (ASSIGN_LOCK) {
				response = transportOrderService.scan(id, request.barcode(), authentication.getName());
			}
			log.info("Transport: order {} scan accepted, status is now {}", id, response.status());
			return response;
		} catch (ResponseStatusException e) {
			log.warn("Transport: order {} scan rejected - {}", id, e.getReason());
			throw e;
		} catch (Exception e) {
			log.error("Transport: exception: " + e.getMessage());
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@PostMapping("/{id}/error")
	public TransportOrderResponse reportError(@PathVariable Long id,
			@Valid @RequestBody TransportOrderErrorRequest request, Authentication authentication) {
		log.info("Transport: order {} user {} reported error {}", id, authentication.getName(), request.reason());
		try {
			TransportOrderResponse response;
			synchronized (ASSIGN_LOCK) {
				response = transportOrderService.reportError(id, request.reason(), authentication.getName());
			}
			log.info("Transport: order {} status is now {}", id, response.status());
			return response;
		} catch (ResponseStatusException e) {
			log.warn("Transport: order {} error report rejected {}", id, e.getReason());
			throw e;
		} catch (Exception e) {
			log.error("Transport: exception: " + e.getMessage());
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}
}
