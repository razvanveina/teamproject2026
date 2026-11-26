package ro.demo.usersbackend.dto;

import java.util.List;

public record OrderCheckResponse(String orderId, String status, String result, List<OrderCheckLine> lines) {
}
