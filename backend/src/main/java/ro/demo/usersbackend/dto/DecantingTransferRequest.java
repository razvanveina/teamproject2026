package ro.demo.usersbackend.dto;

public record DecantingTransferRequest(String loadUnitBarcode, String toteBarcode, String packagingBarcode,
		int quantity) {
}