package ro.demo.usersbackend.dialog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

class DialogConfigLoaderTest {

	private static final String USERS_INLINE = """
			{ "dialogs": [ { "id": "users", "title": "Utilizatori", "entity": "users",
			                 "columns": [ { "field": "id", "label": "ID" } ] } ] }
			""";

	@Test
	void loadsUsersDialogFromClasspathWithFileResolvedAndDefaultsFilled() {
		List<DialogConfig> dialogs = DialogConfigLoader.fromClasspath(new ObjectMapper(), "dialogs").load();

		assertThat(dialogs.get(0)).isEqualTo(new DialogConfig("users", "Utilizatori", "users", List.of(
				new DialogColumn("id", "ID", "number", true),
				new DialogColumn("name", "Nume", "text", true),
				new DialogColumn("email", "Email", "text", true))));
	}

	@Test
	void serviceReturnsSummariesAndNotFoundForUnknownId() {
		DialogService service = new DialogService(new ObjectMapper());

		assertThat(service.findAll().get(0)).isEqualTo(new DialogSummary("users", "Utilizatori"));
		assertThat(service.findById("users").title()).isEqualTo("Utilizatori");
		assertThatThrownBy(() -> service.findById("nu-exista"))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
	}

	@Test
	void missingLocalFileIsNotAnError() {
		List<DialogConfig> dialogs = load(Map.of("dialogs.json", USERS_INLINE));

		assertThat(dialogs).extracting(DialogConfig::id).containsExactly("users");
	}

	@Test
	void localFileAddsItsDialogsAtTheEnd() {
		List<DialogConfig> dialogs = load(Map.of(
				"dialogs.json", USERS_INLINE,
				"dialogs-local.json", """
						{ "dialogs": [ { "file": "articles.json" } ] }
						""",
				"articles.json", """
						{ "id": "articles", "title": "Articole (test)", "entity": "dummy/articles",
						  "columns": [ { "field": "barcode", "label": "Barcode", "sortable": false } ] }
						"""));

		assertThat(dialogs).extracting(DialogConfig::id).containsExactly("users", "articles");
		assertThat(dialogs.get(1).columns().get(0)).isEqualTo(new DialogColumn("barcode", "Barcode", "text", false));
	}

	@Test
	void rejectsMissingMainFile() {
		assertLoadFails(Map.of(), "dialogs/dialogs.json", "file not found");
	}

	@Test
	void rejectsMissingReferencedFile() {
		assertLoadFails(Map.of("dialogs.json", """
				{ "dialogs": [ { "file": "missing.json" } ] }
				"""), "dialogs/dialogs.json, dialogs[0]", "dialogs/missing.json not found");
	}

	@Test
	void rejectsMissingTitle() {
		assertLoadFails(Map.of("dialogs.json", USERS_INLINE.replace("\"title\": \"Utilizatori\",", "")),
				"dialogs/dialogs.json, dialogs[0]", "'title' is missing");
	}

	@Test
	void rejectsInvalidId() {
		assertLoadFails(Map.of("dialogs.json", USERS_INLINE.replace("\"id\": \"users\"", "\"id\": \"Users!\"")),
				"dialogs/dialogs.json, dialogs[0]", "'id' is 'Users!'");
	}

	@Test
	void rejectsDuplicateIdBetweenMainAndLocalFile() {
		assertLoadFails(Map.of("dialogs.json", USERS_INLINE, "dialogs-local.json", USERS_INLINE),
				"dialogs/dialogs-local.json, dialogs[0]", "id 'users' is already used in dialogs/dialogs.json");
	}

	@Test
	void rejectsDialogWithoutColumns() {
		assertLoadFails(Map.of("dialogs.json", """
				{ "dialogs": [ { "id": "users", "title": "Utilizatori", "entity": "users", "columns": [] } ] }
				"""), "dialogs/dialogs.json, dialogs[0]", "at least one column");
	}

	@Test
	void rejectsColumnWithoutLabel() {
		assertLoadFails(Map.of("dialogs.json", USERS_INLINE, "dialogs-local.json", """
				{ "dialogs": [ { "file": "broken.json" } ] }
				""", "broken.json", """
				{ "id": "broken", "title": "Broken", "entity": "broken",
				  "columns": [ { "field": "id", "label": "ID" }, { "field": "name" } ] }
				"""), "dialogs/broken.json", "'columns[1].label' is missing");
	}

	@Test
	void rejectsUnknownColumnType() {
		assertLoadFails(Map.of("dialogs.json", USERS_INLINE.replace("\"label\": \"ID\"", "\"label\": \"ID\", \"type\": \"integer\"")),
				"dialogs/dialogs.json, dialogs[0]", "'columns[0].type' must be one of");
	}

	private static List<DialogConfig> load(Map<String, String> files) {
		DialogConfigLoader loader = new DialogConfigLoader(new ObjectMapper(), "dialogs",
				fileName -> files.containsKey(fileName)
						? new ByteArrayInputStream(files.get(fileName).getBytes(StandardCharsets.UTF_8))
						: null);
		return loader.load();
	}

	private static void assertLoadFails(Map<String, String> files, String expectedSource, String expectedProblem) {
		assertThatThrownBy(() -> load(files))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining(expectedSource)
				.hasMessageContaining(expectedProblem);
	}
}
