package ro.demo.usersbackend.dto;

public record SimItemResponse(String article, String packaging, int quantity, int piecesPerBox, String orderId) {
}