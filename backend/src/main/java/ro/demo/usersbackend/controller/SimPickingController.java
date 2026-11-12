package ro.demo.usersbackend.controller;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ro.demo.usersbackend.dto.SimConfirmRequest;
import ro.demo.usersbackend.dto.SimStationResponse;
import ro.demo.usersbackend.service.SimPickingService;

// adresele statiei de picking din simulator, logica e in SimPickingService
@RestController
@RequestMapping("/api/sim-picking")
public class SimPickingController {
	private final SimPickingService simPickingService;
	public static final Logger log = LogManager.getLogger("application");

	public SimPickingController(SimPickingService simPickingService) {
		this.simPickingService = simPickingService;
	}

	@GetMapping("/station")
	public SimStationResponse findStation() {
		return simPickingService.findStation();
	}
	
	@PostMapping("/confirm")
	public ResponseEntity<Map<String, String>> confirm(@RequestBody SimConfirmRequest request, Authentication authentication) {
		log.info("Sim picking: {} confirmed tote {} into tote {}", authentication.getName(), request.sourceTote(), request.targetTote());
		try {
			return ResponseEntity.ok(Map.of("message", simPickingService.confirm(request.sourceTote(), request.targetTote())));
		} catch (ResponseStatusException e) {
			log.error("Sim picking: {}", e.getReason());
			return ResponseEntity.badRequest().body(Map.of("message", e.getReason()));
		}
	}

}
