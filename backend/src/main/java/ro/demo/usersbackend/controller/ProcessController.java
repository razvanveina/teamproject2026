package ro.demo.usersbackend.controller;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ro.demo.usersbackend.dto.ProcessResponse;
import ro.demo.usersbackend.server.process.ProcessManager;

@RestController
@RequestMapping("/api/processes")
public class ProcessController {
	public static final Logger log = LogManager.getLogger("application");

	@GetMapping
	public List<ProcessResponse> findAll() {
		log.info("Processes: listed all processes");
		return ProcessManager.findAll();
	}

	@PostMapping("/{name}/start")
	public ProcessResponse start(@PathVariable String name) {
		log.info("Processes: started {}", name);
		return found(name, ProcessManager.start(name));
	}

	@PostMapping("/{name}/pause")
	public ProcessResponse pause(@PathVariable String name) {
		log.info("Processes: paused {}", name);
		return found(name, ProcessManager.pause(name));
	}

	@PostMapping("/{name}/stop")
	public ProcessResponse stop(@PathVariable String name) {
		log.info("Processes: stopped {}", name);
		return found(name, ProcessManager.stop(name));
	}

	private ProcessResponse found(String name, ProcessResponse response) {
		if (response == null) {
			log.warn("Processes: process {} not found", name);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "process not found");
		}
		return response;
	}
}