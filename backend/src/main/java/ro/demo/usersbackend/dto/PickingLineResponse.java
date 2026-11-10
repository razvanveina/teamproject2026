package ro.demo.usersbackend.dto;

import java.util.List;

public record PickingLineResponse(String barcode, String description, int quantity, int moved, String status, List<PickingStepResponse> steps) {
}
