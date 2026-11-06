package ro.demo.usersbackend.dto;

import java.util.List;

public record LoadUnitResponse(String barcode, String loadUnitType, String location, // TODO
		List<StockItemResponse> items) {
}