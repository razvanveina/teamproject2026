package ro.demo.usersbackend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import ro.demo.usersbackend.dto.InventoryLoadUnitResponse;
import ro.demo.usersbackend.dto.InventoryOrderLineResponse;
import ro.demo.usersbackend.dto.InventoryOrderLineStartRequest;
import ro.demo.usersbackend.dto.InventoryOrderResponse;
import ro.demo.usersbackend.dto.ScannedInventoryItemRequest;
import ro.demo.usersbackend.service.InventoryOrderService;

@RestController
@RequestMapping("/api/inventory-orders")
@CrossOrigin(origins = "http://localhost:4200")
public class InventoryOrderController {
	private final InventoryOrderService inventoryOrderService;

	public InventoryOrderController(InventoryOrderService inventoryOrderService) {
		this.inventoryOrderService = inventoryOrderService;
	}

	@GetMapping("/load-units/{barcode}")
	public InventoryLoadUnitResponse getEligibleLoadUnit(@PathVariable String barcode) {
		return inventoryOrderService.getEligibleLoadUnit(barcode);
	}

	@GetMapping("/started")
	public List<InventoryOrderResponse> getStartedOrders() {
		return inventoryOrderService.getStartedInventoryOrders();
	}

	@GetMapping("/code/{code}")
	public InventoryOrderResponse getByCode(@PathVariable String code) {
		return inventoryOrderService.getStartedInventoryOrder(code);
	}

	@PostMapping
	public ResponseEntity<InventoryOrderResponse> create() {
		return ResponseEntity.status(HttpStatus.CREATED).body(inventoryOrderService.createInventoryOrder());
	}

	@PostMapping("/{orderId}/lines")
	public ResponseEntity<InventoryOrderLineResponse> startLine(@PathVariable Long orderId,
			@Valid @RequestBody InventoryOrderLineStartRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(inventoryOrderService.startInventoryOrderLine(orderId, request));
	}

	@PutMapping("/{orderId}/lines/{lineId}/items")
	public InventoryOrderLineResponse saveLineItems(@PathVariable Long orderId, @PathVariable Long lineId,
			@NotNull @Valid @RequestBody List<@Valid ScannedInventoryItemRequest> items) {
		return inventoryOrderService.saveInventoryOrderLineItems(orderId, lineId, items);
	}

	@PostMapping("/{orderId}/lines/{lineId}/finish")
	public InventoryOrderLineResponse finishLine(@PathVariable Long orderId, @PathVariable Long lineId) {
		return inventoryOrderService.finishInventoryOrderLine(orderId, lineId);
	}

	@PostMapping("/{orderId}/lines/{lineId}/finish-empty")
	public InventoryOrderLineResponse finishEmptyLine(@PathVariable Long orderId, @PathVariable Long lineId) {
		return inventoryOrderService.finishEmptyInventoryOrderLine(orderId, lineId);
	}

	@PostMapping("/{orderId}/finish")
	public ResponseEntity<Void> finishOrder(@PathVariable Long orderId) {
		inventoryOrderService.finishInventoryOrder(orderId);
		return ResponseEntity.noContent().build();
	}
}
