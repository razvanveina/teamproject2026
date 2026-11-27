package ro.demo.usersbackend.controller;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ro.demo.usersbackend.service.DatabaseCleanupService;

@RestController
@RequestMapping("/api/host-testing")
public class DatabaseCleanupController {
	private static final Logger log = LogManager.getLogger(DatabaseCleanupController.class);

	private final DatabaseCleanupService databaseCleanupService = new DatabaseCleanupService();

	@PostMapping("/clean-database")
	public Map<String, String> cleanDatabase() {
		log.info("Testing: database cleanup requested");
		databaseCleanupService.cleanWorkingData();
		return Map.of("message", "Working data removed. Press Reset in the simulator too.");
	}
}