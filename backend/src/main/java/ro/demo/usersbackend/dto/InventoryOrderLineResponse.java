package ro.demo.usersbackend.dto;

import java.util.List;

public record InventoryOrderLineResponse(Long id, Long loadUnitId, String loadUnitBarcode, String loadUnitType,
		String locationName, String status, List<ScannedInventoryItemRequest> items) {
}
