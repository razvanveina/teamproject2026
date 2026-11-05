package ro.demo.usersbackend.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ro.demo.usersbackend.dto.LogRequest;

@RestController
@RequestMapping("/api/log")
@CrossOrigin(origins = "http://localhost:4200")
public class LogController {
	public static final Logger log = LogManager.getLogger("application");

	@PostMapping
	public ResponseEntity<Void> log(@Valid @RequestBody LogRequest request) {
		log.info("User pressed {}", request.action());
		return ResponseEntity.noContent().build();
	}
}
