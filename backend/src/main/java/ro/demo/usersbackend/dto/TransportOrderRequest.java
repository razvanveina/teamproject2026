package ro.demo.usersbackend.dto;

import jakarta.validation.constraints.NotBlank;

public record TransportOrderRequest(@NotBlank String barcode) {
}
