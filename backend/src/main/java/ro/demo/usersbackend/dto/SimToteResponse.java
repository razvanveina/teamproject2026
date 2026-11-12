package ro.demo.usersbackend.dto;

import java.util.List;

public record SimToteResponse(String barcode, List<SimItemResponse> items) {
}