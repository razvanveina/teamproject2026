package ro.demo.usersbackend.dto;

public record InventoryLoadUnitResponse(Long id, String barcode, String loadUnitType, String locationName) {
}
