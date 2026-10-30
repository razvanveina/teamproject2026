package ro.demo.usersbackend.security;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import ro.demo.usersbackend.entity.Account;
import ro.demo.usersbackend.repository.AccountRepository;

@Component
public class Admin implements CommandLineRunner{
	private static final String USERNAME = "123";
	private static final String PASSWORD = "123";
	private final AccountRepository accounts;
	private final PasswordEncoder encoder;
	public Admin(AccountRepository accounts, PasswordEncoder encoder) {
		super();
		this.accounts = accounts;
		this.encoder = encoder;
	}
	
	@Override
	public void run(String... args) {
		if (!accounts.existsByUsername(USERNAME)) {
			accounts.save(new Account(USERNAME, encoder.encode(PASSWORD)));
		}
	}
	
}
