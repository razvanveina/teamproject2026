package ro.demo.usersbackend.dto;

import jakarta.validation.constraints.NotBlank;

public record InventoryOrderLineStartRequest(@NotBlank String loadUnitBarcode) {
}
