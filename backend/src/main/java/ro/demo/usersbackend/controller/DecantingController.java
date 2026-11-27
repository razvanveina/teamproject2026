package ro.demo.usersbackend.controller;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ro.demo.usersbackend.dto.DecantingResponse;
import ro.demo.usersbackend.dto.DecantingStartRequest;
import ro.demo.usersbackend.dto.DecantingTransferRequest;
import ro.demo.usersbackend.service.DecantingService;

@RestController
@RequestMapping("/api/decanting")
@CrossOrigin(origins = "http://localhost:4200")
public class DecantingController {
	private static final Logger log = LogManager.getLogger(DecantingController.class);
	private final DecantingService decantingService;

	public DecantingController(DecantingService decantingService) {
		this.decantingService = decantingService;
	}

	@PostMapping("/tote")
	public Map<String, String> createEmptyTote() {
		String barcode = decantingService.createEmptyTote();
		log.info("Decanting: empty tote {} created", barcode);
		return Map.of("toteBarcode", barcode);
	}

	@PostMapping("/start")
	public DecantingResponse start(@RequestBody DecantingStartRequest request) {
		log.info("Decanting: user scanned load unit {}", request.loadUnitBarcode());
		try {
			return decantingService.start(request.loadUnitBarcode());
		} catch (ResponseStatusException e) {
			log.warn("Decanting: start rejected {}", e.getReason());
			throw e;
		}
	}

	@PostMapping("/transfer")
	public DecantingResponse transfer(@RequestBody DecantingTransferRequest request) {
		log.info("Decanting: user moved {} {} from load unit {}", request.quantity(), request.packagingBarcode(),
				request.loadUnitBarcode());
		try {
			return decantingService.transfer(request);
		} catch (ResponseStatusException e) {
			log.warn("Decanting: transfer rejected {}", e.getReason());
			throw e;
		}
	}

	@GetMapping("/load-units")
	public List<Map<String, String>> loadUnitsAtDecanting() {
		return decantingService.findLoadUnitsAtDecanting();
	}
}