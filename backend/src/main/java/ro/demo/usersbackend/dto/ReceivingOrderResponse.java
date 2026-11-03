package ro.demo.usersbackend.dto;

import java.util.List;

public record ReceivingOrderResponse(String orderId, String status, List<ReceivingOrderLineResponse> lines) {

}
