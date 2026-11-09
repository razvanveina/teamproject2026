package ro.demo.usersbackend.dto;

public record PickingStepResponse(String location, String barcode, int quantity, int piecesPerBox) {
}
