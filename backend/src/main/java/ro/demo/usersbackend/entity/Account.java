package ro.demo.usersbackend.entity;

import java.time.Instant;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
@NamedQueries({ //
		@NamedQuery(name = "Account.findByUsername", //
				query = "from Account a where a.username = :username"), //
		@NamedQuery(name = "Account.findAllOrderByUsername", //
				query = "from Account a order by a.username")//
})
public class Account {
	public static final String BY_USERNAME = "Account.findByUsername";
	public static final String ALL_ORDER_BY_USERNAME = "Account.findAllOrderByUsername";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 50)
	private String username;

	@Column(nullable = false, length = 100)
	private String passwordHash;

	private Instant lastSeen;

	public Account() {
		super();
	}

	public Account(String username, String passwordHash) {
		this.username = username;
		this.passwordHash = passwordHash;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public Instant getLastSeen() {
		return lastSeen;
	}

	public void setLastSeen(Instant lastSeen) {
		this.lastSeen = lastSeen;
	}

}