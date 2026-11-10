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

@RestController
@RequestMapping("/api/events")
public class EventController {
	public static final Logger log = LogManager.getLogger("application");

	private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

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
}
