package ro.demo.usersbackend.dto;

public record DecantingItem(String packagingBarcode, String articleBarcode, String description,
		String packaging, int quantity, int reservedQuantity) {
}
