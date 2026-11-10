package ro.demo.usersbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
		@NotBlank @Size(max = 50) String username,
		@NotBlank @Size(min = 3, max = 20) String password) {
}