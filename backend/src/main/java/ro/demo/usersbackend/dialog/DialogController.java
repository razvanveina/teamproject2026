package ro.demo.usersbackend.dialog;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dialogs")
public class DialogController {

	private final DialogService dialogService;

	public DialogController(DialogService dialogService) {
		this.dialogService = dialogService;
	}

	@GetMapping
	public List<DialogSummary> findAll() {
		return dialogService.findAll();
	}

	@GetMapping("/{id}")
	public DialogConfig findById(@PathVariable String id) {
		return dialogService.findById(id);
	}
}
