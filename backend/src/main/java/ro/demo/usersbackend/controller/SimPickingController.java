package ro.demo.usersbackend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ro.demo.usersbackend.dto.SimStationResponse;
import ro.demo.usersbackend.service.SimPickingService;

// adresele statiei de picking din simulator, logica e in SimPickingService
@RestController
@RequestMapping("/api/sim-picking")
public class SimPickingController {
	private final SimPickingService simPickingService;

	public SimPickingController(SimPickingService simPickingService) {
		this.simPickingService = simPickingService;
	}

	@GetMapping("/station")
	public SimStationResponse findStation() {
		return simPickingService.findStation();
	}

}
