package ro.demo.usersbackend.repository;

import java.util.Optional;

import org.hibernate.Session;

import ro.demo.usersbackend.entity.Account;

public class AccountRepository extends BaseRepository<Account, Long> {
	public AccountRepository(Session session) {
		super(session, Account.class);
	}

	public Optional<Account> findByUsername(String username) {
		return session.createNamedQuery(Account.BY_USERNAME, Account.class).setParameter("username", username)
				.uniqueResultOptional();
	}

	public boolean existsByUsername(String username) {
		return findByUsername(username).isPresent();
	}
}