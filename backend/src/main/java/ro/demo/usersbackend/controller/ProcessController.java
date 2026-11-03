package ro.demo.usersbackend.controller;

import java.util.List;

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

	@GetMapping
	public List<ProcessResponse> findAll() {
		return ProcessManager.findAll();
	}

	@PostMapping("/{name}/start")
	public ProcessResponse start(@PathVariable String name) {
		return found(ProcessManager.start(name));
	}

	@PostMapping("/{name}/pause")
	public ProcessResponse pause(@PathVariable String name) {
		return found(ProcessManager.pause(name));
	}

	@PostMapping("/{name}/stop")
	public ProcessResponse stop(@PathVariable String name) {
		return found(ProcessManager.stop(name));
	}

	private ProcessResponse found(ProcessResponse response) {
		if (response == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "process not found");
		}
		return response;
	}
}