package ro.demo.usersbackend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ro.demo.usersbackend.dto.AuthResponse;
import ro.demo.usersbackend.dto.LoginRequest;
import ro.demo.usersbackend.dto.RegisterRequest;
import ro.demo.usersbackend.service.AuthService;

@RestController
@RequestMapping("/auth")
public class AuthController {
	private final AuthService authService;

	public AuthController(AuthService authService) {
		super();
		this.authService = authService;
	}

	//@PostMapping("/register")
	//@ResponseStatus(HttpStatus.CREATED)
	//public void register(@Valid @RequestBody RegisterRequest request) {
	//	authService.register(request);
	//}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}
}