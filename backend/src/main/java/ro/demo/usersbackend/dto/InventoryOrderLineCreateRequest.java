package ro.demo.usersbackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InventoryOrderLineCreateRequest(@NotNull @Positive Integer locationId) {
}
