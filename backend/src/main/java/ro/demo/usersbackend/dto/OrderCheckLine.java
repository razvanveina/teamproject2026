package ro.demo.usersbackend.dto;

import java.util.List;

public record OrderCheckLine(String article, String description, int quantity, int reserved, int free, int missing,
		List<ReservedLoadUnit> loadUnits) {
}
