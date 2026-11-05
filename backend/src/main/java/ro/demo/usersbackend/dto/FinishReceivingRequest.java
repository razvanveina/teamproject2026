package ro.demo.usersbackend.dto;

import java.util.List;

public record FinishReceivingRequest(String orderId, List<ScannedPackage> packages){
}
