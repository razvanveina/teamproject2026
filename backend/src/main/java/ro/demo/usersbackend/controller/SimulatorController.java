package ro.demo.usersbackend.controller;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ro.demo.usersbackend.service.SimulatorService;

@RestController
@RequestMapping("/api/simulator")
public class SimulatorController {
	private static final Logger log = LogManager.getLogger("application");

	private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

	private final SimulatorService simulatorService;

	public SimulatorController(SimulatorService simulatorService) {
		super();
		this.simulatorService = simulatorService;
	}

	@GetMapping("/subscribe")
	public SseEmitter stream() {
		SseEmitter emitter = new SseEmitter();

		emitters.add(emitter);

		emitter.onCompletion(() -> emitters.remove(emitter));
		emitter.onTimeout(() -> emitters.remove(emitter));

		return emitter;
	}

	public void sendUpdate(Object data) {
		for (SseEmitter emitter : emitters) {
			try {
				emitter.send(data);
			} catch (Exception e) {
				emitters.remove(emitter);
			}
		}
	}

	@PostMapping("/sendEvent")
	public ResponseEntity<Void> sendEvent(@RequestBody String name) {
		log.info("Event requested: {}", name);
		sendUpdate(name);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/entities")
	public ResponseEntity<List<String>> getEntities() {
		log.info("Requested entities from the simulator");

		return ResponseEntity.ok(simulatorService.getEntities());
	}

//	@PostMapping("/createLU")
//	public ResponseEntity<Void> createLU(@PathVariable String barcode, @PathVariable String location) {
//		context.createProducer().send(outQueue, message);
//		System.out.println(message);
//		return ResponseEntity.ok().build();
//	}
}
