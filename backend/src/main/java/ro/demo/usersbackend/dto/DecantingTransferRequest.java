package ro.demo.usersbackend.dto;

public record DecantingTransferRequest(String loadUnitBarcode, String packagingBarcode, int quantity) {
}