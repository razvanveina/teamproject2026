package ro.demo.usersbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// ce trimite client-ul pt register
public record RegisterRequest(@NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[A-Za-z0-9_.-]+$") String username,
		@NotBlank @Size(min = 15, max = 72) String password) {
}