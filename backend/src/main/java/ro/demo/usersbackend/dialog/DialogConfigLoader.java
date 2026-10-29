package ro.demo.usersbackend.dialog;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Reads the dialog configuration files (dialogs.json, the files it references and the optional
 * dialogs-local.json), fills in the default values and checks the rules from API.md §2.
 * Any problem stops the loading with a message that names the file and the field.
 */
public class DialogConfigLoader {

	public static final String MAIN_FILE = "dialogs.json";
	public static final String LOCAL_FILE = "dialogs-local.json";

	private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9-]+");
	private static final Set<String> TYPES = Set.of("text", "number", "date", "boolean");

	private final ObjectMapper objectMapper;
	private final String folder;
	// Opens a file from the folder by name, or returns null if the file does not exist.
	private final Function<String, InputStream> openFile;

	public DialogConfigLoader(ObjectMapper objectMapper, String folder, Function<String, InputStream> openFile) {
		this.objectMapper = objectMapper;
		this.folder = folder;
		this.openFile = openFile;
	}

	public static DialogConfigLoader fromClasspath(ObjectMapper objectMapper, String folder) {
		ClassLoader classLoader = DialogConfigLoader.class.getClassLoader();
		return new DialogConfigLoader(objectMapper, folder,
				fileName -> classLoader.getResourceAsStream(folder + "/" + fileName));
	}

	public List<DialogConfig> load() {
		List<DialogConfig> dialogs = new ArrayList<>();
		Map<String, String> sourceById = new HashMap<>();

		readList(MAIN_FILE, true, dialogs, sourceById);
		readList(LOCAL_FILE, false, dialogs, sourceById);

		return List.copyOf(dialogs);
	}

	private void readList(String fileName, boolean required, List<DialogConfig> dialogs,
			Map<String, String> sourceById) {
		JsonNode root = readFile(fileName);
		if (root == null) {
			if (required) {
				throw error(path(fileName), "file not found");
			}
			return;
		}

		JsonNode entries = root.get("dialogs");
		if (entries == null || !entries.isArray()) {
			throw error(path(fileName), "'dialogs' must be a list");
		}

		for (int i = 0; i < entries.size(); i++) {
			JsonNode entry = entries.get(i);
			String source = path(fileName) + ", dialogs[" + i + "]";

			if (entry.has("file")) {
				if (entry.size() > 1) {
					throw error(source, "an entry with 'file' must not have other fields");
				}
				String referencedFile = requiredText(entry, "file", source);
				entry = readFile(referencedFile);
				if (entry == null) {
					throw error(source, "referenced file " + path(referencedFile) + " not found");
				}
				source = path(referencedFile);
			}

			DialogConfig dialog = toDialog(entry, source);

			String previousSource = sourceById.putIfAbsent(dialog.id(), source);
			if (previousSource != null) {
				throw error(source, "id '" + dialog.id() + "' is already used in " + previousSource);
			}
			dialogs.add(dialog);
		}
	}

	private DialogConfig toDialog(JsonNode node, String source) {
		if (!node.isObject()) {
			throw error(source, "a dialog must be a JSON object");
		}

		String id = requiredText(node, "id", source);
		if (!ID_PATTERN.matcher(id).matches()) {
			throw error(source, "'id' is '" + id + "', but it may only contain a-z, 0-9 and -");
		}
		String title = requiredText(node, "title", source);
		String entity = requiredText(node, "entity", source);

		JsonNode columnNodes = node.get("columns");
		if (columnNodes == null || !columnNodes.isArray() || columnNodes.isEmpty()) {
			throw error(source, "'columns' must be a list with at least one column");
		}

		List<DialogColumn> columns = new ArrayList<>();
		for (int i = 0; i < columnNodes.size(); i++) {
			columns.add(toColumn(columnNodes.get(i), source, "columns[" + i + "]"));
		}

		return new DialogConfig(id, title, entity, List.copyOf(columns));
	}

	private DialogColumn toColumn(JsonNode node, String source, String name) {
		if (!node.isObject()) {
			throw error(source, "'" + name + "' must be a JSON object");
		}

		String field = requiredText(node, "field", source, name + ".field");
		if (field.contains(".")) {
			throw error(source, "'" + name + ".field' is '" + field + "', but it must be a direct field, without dots");
		}
		String label = requiredText(node, "label", source, name + ".label");

		String type = "text";
		JsonNode typeNode = node.get("type");
		if (typeNode != null) {
			type = typeNode.asText();
			if (!typeNode.isTextual() || !TYPES.contains(type)) {
				throw error(source, "'" + name + ".type' must be one of " + TYPES);
			}
		}

		boolean sortable = true;
		JsonNode sortableNode = node.get("sortable");
		if (sortableNode != null) {
			if (!sortableNode.isBoolean()) {
				throw error(source, "'" + name + ".sortable' must be true or false");
			}
			sortable = sortableNode.asBoolean();
		}

		return new DialogColumn(field, label, type, sortable);
	}

	private JsonNode readFile(String fileName) {
		try (InputStream in = openFile.apply(fileName)) {
			if (in == null) {
				return null;
			}
			return objectMapper.readTree(in);
		} catch (IOException e) {
			throw new IllegalStateException(path(fileName) + ": invalid JSON: " + e.getMessage(), e);
		}
	}

	private String requiredText(JsonNode node, String key, String source) {
		return requiredText(node, key, source, key);
	}

	private String requiredText(JsonNode node, String key, String source, String name) {
		JsonNode value = node.get(key);
		if (value == null || !value.isTextual() || value.asText().isBlank()) {
			throw error(source, "'" + name + "' is missing or empty");
		}
		return value.asText();
	}

	private String path(String fileName) {
		return folder + "/" + fileName;
	}

	private IllegalStateException error(String source, String problem) {
		return new IllegalStateException("Invalid dialog configuration in " + source + ": " + problem);
	}
}
