package ro.demo.usersbackend.dto;

public record ReceivingOrderLineResponse(long id, String description, int quantity, int receivedQuantity,
		String barcode, String status) {

}
