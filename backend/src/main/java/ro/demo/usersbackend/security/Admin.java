package ro.demo.usersbackend.security;

import org.hibernate.Session;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.entity.Account;
import ro.demo.usersbackend.repository.AccountRepository;

@Component
public class Admin implements CommandLineRunner {
	private static final String USERNAME = "123";
	private static final String PASSWORD = "123";
	private final PasswordEncoder encoder;

	public Admin(PasswordEncoder encoder) {
		this.encoder = encoder;
	}

	@Override
	public void run(String... args) {
		Account admin = new Account(USERNAME, encoder.encode(PASSWORD));
		new WithSessionAndTransaction<>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				AccountRepository ar = new AccountRepository(session);
				if (!ar.existsByUsername(USERNAME)) {
					ar.insert(admin);
				}
			}
		}.run();
	}
}