package ro.demo.usersbackend.dto;

import java.util.List;

public record DecantingResponse(String loadUnitBarcode, String toteBarcode, List<DecantingItem> items,
		boolean finished) {
}