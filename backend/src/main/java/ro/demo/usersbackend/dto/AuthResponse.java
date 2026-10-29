package ro.demo.usersbackend.dto;

// ce returneaza login-ul
public record AuthResponse(String token, String username) {

}