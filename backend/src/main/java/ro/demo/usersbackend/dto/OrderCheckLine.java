package ro.demo.usersbackend.dto;

public record OrderCheckLine(String article, String description, int needed, int free, int missing) {
}
