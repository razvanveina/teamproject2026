package ro.demo.usersbackend.dto;

import java.util.List;

public record PickingOrderResponse(String orderId, String status, String destination, String box, List<PickingLineResponse> lines) {
}
