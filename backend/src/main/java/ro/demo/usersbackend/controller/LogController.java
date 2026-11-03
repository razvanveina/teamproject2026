package ro.demo.usersbackend.controller;

import static ro.demo.usersbackend.logging.ActionLog.LOG;

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

	@PostMapping
	public ResponseEntity<Void> log(@Valid @RequestBody LogRequest request) {
		LOG.info("User pressed {}", request.action());
		return ResponseEntity.noContent().build();
	}
}
