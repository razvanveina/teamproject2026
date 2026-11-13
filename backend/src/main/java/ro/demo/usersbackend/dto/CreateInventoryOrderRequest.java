package ro.demo.usersbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateInventoryOrderRequest(@NotBlank @Size(max = 100) String code) {
}
