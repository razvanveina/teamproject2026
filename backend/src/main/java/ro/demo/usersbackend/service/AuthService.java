package ro.demo.usersbackend.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ro.demo.usersbackend.dto.AuthResponse;
import ro.demo.usersbackend.dto.LoginRequest;
import ro.demo.usersbackend.dto.RegisterRequest;
import ro.demo.usersbackend.entity.Account;
import ro.demo.usersbackend.repository.AccountRepository;
import ro.demo.usersbackend.security.JwtService;

@Service
public class AuthService {
	private final AccountRepository accounts;
	private final PasswordEncoder encoder;
	private final JwtService jwtService;

	public AuthService(AccountRepository accounts, PasswordEncoder encoder, JwtService jwtService) {
		super();
		this.accounts = accounts;
		this.encoder = encoder;
		this.jwtService = jwtService;
	}

	public void register(RegisterRequest request) {
		if (accounts.existsByUsername(request.username())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "username already taken");
		}

		accounts.save(new Account(request.username(), encoder.encode(request.password())));
	}

	public AuthResponse login(LoginRequest request) {
		Account account = accounts.findByUsername(request.username())
				.filter(a -> encoder.matches(request.password(), a.getPasswordHash()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials"));
		return new AuthResponse(jwtService.createToken(account.getUsername()), account.getUsername());
	}

}