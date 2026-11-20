package ro.demo.usersbackend.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.dto.AccountResponse;
import ro.demo.usersbackend.dto.CreateAccountRequest;
import ro.demo.usersbackend.entity.Account;
import ro.demo.usersbackend.repository.AccountRepository;

@Service
public class AccountService {
	private static final Duration ONLINE_WINDOW = Duration.ofMinutes(1);

	private final PasswordEncoder encoder;

	public AccountService(PasswordEncoder encoder) {
		this.encoder = encoder;
	}

	private AccountResponse toResponse(Account a) {
		boolean online = a.getLastSeen() != null && a.getLastSeen().isAfter(Instant.now().minus(ONLINE_WINDOW));
		return new AccountResponse(a.getId(), a.getUsername(), online);
	}

	public List<AccountResponse> findAll() {
		List<Account> accounts = new WithSessionAndTransaction<List<Account>>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				setReturnValue(new AccountRepository(session).findAllOrderByUsername());
			}
		}.run();
		return accounts.stream().map(this::toResponse).toList();
	}

	public AccountResponse create(CreateAccountRequest request) {
		String username = request.username().trim();
		Account account = new Account(username, encoder.encode(request.password()));
		boolean created = new WithSessionAndTransaction<Boolean>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				AccountRepository ar = new AccountRepository(session);
				if (ar.existsByUsername(username)) {
					setReturnValue(false);
					return;
				}
				ar.insert(account);
				setReturnValue(true);
			}
		}.run();
		if (!created) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Username " + username + " already exists.");
		}
		return toResponse(account);
	}

	public void delete(Long id, String currentUsername) {
		Account account = new WithSessionAndTransaction<Account>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				AccountRepository ar = new AccountRepository(session);
				Account found = ar.findById(id).orElse(null);
				if (found != null && !found.getUsername().equals(currentUsername)) {
					ar.delete(found);
				}
				setReturnValue(found);
			}
		}.run();
		if (account == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account " + id + " not found.");
		}
		if (account.getUsername().equals(currentUsername)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot delete your own account.");
		}
	}

	public void heartbeat(String username) {
		setLastSeen(username, Instant.now());
	}

	public void logout(String username) {
		setLastSeen(username, null);
	}

	private void setLastSeen(String username, Instant lastSeen) {
		new WithSessionAndTransaction<>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				new AccountRepository(session).findByUsername(username).ifPresent(a -> a.setLastSeen(lastSeen));
			}
		}.run();
	}
}