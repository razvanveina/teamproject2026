package ro.demo.usersbackend.dto;

public record PickingErrorRequest(String orderId, Long stockItemId, String box, String reason, int found) {
}
