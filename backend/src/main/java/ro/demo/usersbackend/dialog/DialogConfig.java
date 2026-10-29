package ro.demo.usersbackend.dialog;

import java.util.List;

public record DialogConfig(String id, String title, String entity, List<DialogColumn> columns) {
}
