package ro.demo.usersbackend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record InventoryOrderResponse(Long id, String code, LocalDateTime createdAt, LocalDateTime finishedAt, String status,
		List<InventoryOrderLineResponse> lines) {
}
