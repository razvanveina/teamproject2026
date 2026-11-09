package ro.demo.usersbackend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ro.demo.usersbackend.dto.InventoryCheckRequest;
import ro.demo.usersbackend.service.InventoryCheckService;

@RestController
@RequestMapping("/api/inventory-checks")
@CrossOrigin(origins = "http://localhost:4200")
public class InventoryCheckController {
	private final InventoryCheckService inventoryCheckService;

	public InventoryCheckController(InventoryCheckService inventoryCheckService) {
		this.inventoryCheckService = inventoryCheckService;
	}

	@PostMapping
	public ResponseEntity<Void> create(@Valid @RequestBody InventoryCheckRequest request) {
		inventoryCheckService.saveInventoryCheck(request);
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}
}