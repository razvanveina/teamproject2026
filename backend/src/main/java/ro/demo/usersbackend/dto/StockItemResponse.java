package ro.demo.usersbackend.dto;

public record StockItemResponse(long id, String barcode, int quantity, String articleBarcode, String articleDescription,
		String packagingUnitTypeName, int packagingUnitTypeQuantity) {
}