package ro.demo.usersbackend.dialog;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DialogService {

	private final List<DialogConfig> dialogs;

	public DialogService(ObjectMapper objectMapper) {
		// Loaded once, at startup. An invalid configuration stops the application.
		this.dialogs = DialogConfigLoader.fromClasspath(objectMapper, "dialogs").load();
	}

	public List<DialogSummary> findAll() {
		return dialogs.stream()
				.map(dialog -> new DialogSummary(dialog.id(), dialog.title()))
				.toList();
	}

	public DialogConfig findById(String id) {
		return dialogs.stream()
				.filter(dialog -> dialog.id().equals(id))
				.findFirst()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dialog not found: " + id));
	}
}
