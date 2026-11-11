package ro.demo.usersbackend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ro.demo.usersbackend.dto.InventoryLoadUnitResponse;
import ro.demo.usersbackend.dto.InventoryOrderRequest;
import ro.demo.usersbackend.service.InventoryCheckService;

@RestController
@RequestMapping("/api/inventory-orders")
@CrossOrigin(origins = "http://localhost:4200")
public class InventoryOrderController {
	private final InventoryCheckService inventoryCheckService;

	public InventoryOrderController(InventoryCheckService inventoryCheckService) {
		this.inventoryCheckService = inventoryCheckService;
	}

	@GetMapping("/load-units/{barcode}")
	public InventoryLoadUnitResponse getEligibleLoadUnit(@PathVariable String barcode) {
		return inventoryCheckService.getEligibleLoadUnit(barcode);
	}

	@PostMapping
	public ResponseEntity<Void> create(@Valid @RequestBody InventoryOrderRequest request) {
		inventoryCheckService.saveInventoryOrder(request);
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}
}
