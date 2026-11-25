package ro.demo.usersbackend.service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;

import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.dto.AuthResponse;
import ro.demo.usersbackend.dto.LoginRequest;
import ro.demo.usersbackend.entity.Account;
import ro.demo.usersbackend.repository.AccountRepository;
import ro.demo.usersbackend.security.JwtService;

@Service
public class AuthService {
	private final PasswordEncoder encoder;
	private final JwtService jwtService;
	private final WorkplaceService workplaceService;

	public AuthService(PasswordEncoder encoder, JwtService jwtService, WorkplaceService workplaceService) {
		this.encoder = encoder;
		this.jwtService = jwtService;
		this.workplaceService = workplaceService;
	}

	public AuthResponse login(LoginRequest request, String remoteAddress) {
		Account account = new WithSessionAndTransaction<Account>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				Account found = new AccountRepository(session).findByUsername(request.username())
						.filter(a -> encoder.matches(request.password(), a.getPasswordHash())).orElse(null);
				if (found != null) {
					found.setLastSeen(Instant.now());
				}
				setReturnValue(found);
			}
		}.run();
		if (account == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials");
		}
		return new AuthResponse(
				jwtService.createToken(account.getUsername(), remoteAddress, hostOf(remoteAddress)),
				account.getUsername(),
				workplaceService.findNameFor(remoteAddress));
	}

	private String hostOf(String remoteAddress) {
		try {
			String host = InetAddress.getByName(remoteAddress).getCanonicalHostName();
			return host.equals(remoteAddress) ? null : host;
		} catch (UnknownHostException e) {
			return null;
		}
	}
}
