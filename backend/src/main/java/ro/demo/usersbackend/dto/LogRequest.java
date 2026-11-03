package ro.demo.usersbackend.dto;

import jakarta.validation.constraints.NotBlank;

public record LogRequest(@NotBlank String action) {
}
