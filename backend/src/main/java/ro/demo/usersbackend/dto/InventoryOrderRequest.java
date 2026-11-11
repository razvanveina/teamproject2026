package ro.demo.usersbackend.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record InventoryOrderRequest(@NotEmpty List<@Valid InventoryOrderLineRequest> lines) {
}
