package ro.demo.usersbackend.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InventoryCheckRequest(@NotBlank String loadUnitBarcode, String loadUnitType,
		@NotNull List<@Valid ScannedInventoryItemRequest> items) {
}