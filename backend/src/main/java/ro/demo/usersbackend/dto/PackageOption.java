package ro.demo.usersbackend.dto;

public record PackageOption(String barcode, String articleBarcode, String description, String packagingUnit,
		int quantity) {
}