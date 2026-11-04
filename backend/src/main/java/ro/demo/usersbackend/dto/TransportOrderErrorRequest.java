package ro.demo.usersbackend.dto;

import jakarta.validation.constraints.NotNull;
import ro.demo.usersbackend.entity.TransportOrderErrorReason;

public record TransportOrderErrorRequest(@NotNull TransportOrderErrorReason reason) {
}
