package ro.demo.usersbackend.dto;

public record PickingStepResponse(Long id, String location, String barcode, int quantity, int piecesPerBox) {
}
