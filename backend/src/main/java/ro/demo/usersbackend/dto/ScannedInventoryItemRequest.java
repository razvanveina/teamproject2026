package ro.demo.usersbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record ScannedInventoryItemRequest(@NotBlank String articleBarcode, String articleDescription,
		@NotBlank String packagingUnitName, @PositiveOrZero int quantity) {
}