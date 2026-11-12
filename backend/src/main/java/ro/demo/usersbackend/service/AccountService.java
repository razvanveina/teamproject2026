package ro.demo.usersbackend.service;

import java.time.Duration;
import java.time.Instant;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ro.demo.usersbackend.dto.AccountResponse;
import ro.demo.usersbackend.dto.CreateAccountRequest;
import ro.demo.usersbackend.entity.Account;
import ro.demo.usersbackend.repository.AccountRepository;

@Service
public class AccountService {
	private final AccountRepository accounts;
	private final PasswordEncoder encoder;
	
	private static final Duration ONLINE_WINDOW = Duration.ofMinutes(1);

	public AccountService(AccountRepository accounts, PasswordEncoder encoder) {
		this.accounts = accounts;
		this.encoder = encoder;
	}
	
	private AccountResponse toResponse(Account a) {
		boolean online = a.getLastSeen() != null && a.getLastSeen().isAfter(Instant.now().minus(ONLINE_WINDOW));
		return new AccountResponse(a.getId(), a.getUsername(), online);
	}
	

	@Transactional(readOnly = true)
	public List<AccountResponse> findAll() {
		return accounts.findAll(Sort.by("username")).stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public AccountResponse create(CreateAccountRequest request) {
		String username = request.username().trim();
		if (accounts.existsByUsername(username)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Username " + username + " already exists.");
		}

		Account account = accounts.save(new Account(username, encoder.encode(request.password())));
		return toResponse(account);
	}
	
	@Transactional
	public void delete(Long id, String currentUsername) {
		Account account = accounts.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account " + id + " not found."));

		if (account.getUsername().equals(currentUsername)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot delete your own account.");
		}

		accounts.delete(account);
	}
	
	@Transactional
	public void heartbeat(String username) {
		accounts.findByUsername(username).ifPresent(a -> a.setLastSeen(Instant.now()));
	}

	@Transactional
	public void logout(String username) {
		accounts.findByUsername(username).ifPresent(a -> a.setLastSeen(null));
	}
}