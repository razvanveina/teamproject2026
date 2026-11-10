package ro.demo.usersbackend.dto;

public record PickingRequest(String orderId, String box, String barcode, Long stockItemId) {
}
