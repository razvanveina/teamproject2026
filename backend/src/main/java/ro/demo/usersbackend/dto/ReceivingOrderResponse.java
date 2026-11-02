package ro.demo.usersbackend.dto;

import java.util.List;

import ro.demo.usersbackend.entity.ReceivingOrderLine;

public record ReceivingOrderResponse(String orderId, String status, List<ReceivingOrderLine> lines) {

}
