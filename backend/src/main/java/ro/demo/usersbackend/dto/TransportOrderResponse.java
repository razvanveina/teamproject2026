package ro.demo.usersbackend.dto;

import ro.demo.usersbackend.entity.TransportOrderStatus;

public record TransportOrderResponse(Long id, TransportOrderStatus status, String palletBarcode, String sourceLocation,
		String destinationLocation, String loadUnitLocation) {
}
