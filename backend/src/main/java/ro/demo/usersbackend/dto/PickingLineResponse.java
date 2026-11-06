package ro.demo.usersbackend.dto;

public record PickingLineResponse(String barcode, String description, int quantity, String status, String location) {
}
