package ro.demo.usersbackend.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ro.demo.usersbackend.dto.DecantingResponse;
import ro.demo.usersbackend.dto.DecantingStartRequest;
import ro.demo.usersbackend.dto.DecantingTransferRequest;
import ro.demo.usersbackend.service.DecantingService;

@RestController
@RequestMapping("/api/decanting")
@CrossOrigin(origins = "http://localhost:4200")
public class DecantingController {
	private final DecantingService decantingService;

	public DecantingController(DecantingService decantingService) {
		this.decantingService = decantingService;
	}

	@PostMapping("/start")
	public DecantingResponse start(@RequestBody DecantingStartRequest request) {
		return decantingService.start(request.loadUnitBarcode());
	}

	@PostMapping("/transfer")
	public DecantingResponse transfer(@RequestBody DecantingTransferRequest request) {
		return decantingService.transfer(request);
	}
}