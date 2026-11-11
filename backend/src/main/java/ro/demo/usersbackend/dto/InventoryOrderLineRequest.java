package ro.demo.usersbackend.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InventoryOrderLineRequest(@NotBlank String loadUnitBarcode,
		@NotNull List<@Valid ScannedInventoryItemRequest> items) {
}
