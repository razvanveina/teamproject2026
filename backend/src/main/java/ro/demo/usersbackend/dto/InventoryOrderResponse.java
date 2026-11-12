package ro.demo.usersbackend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record InventoryOrderResponse(Long id, LocalDateTime createdAt, LocalDateTime finishedAt, String status,
		List<InventoryOrderLineResponse> lines) {
}
