package ro.demo.usersbackend.entity;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
@NamedQueries({ //
		@NamedQuery(name = Destination.FIND_ALL, //
				query = "from Destination d order by d.name"), //
		@NamedQuery(name = Destination.BY_NAME, //
				query = "from Destination d where d.name = :name") })
public class Destination {
	public static final String FIND_ALL = "Destination.findAll";
	public static final String BY_NAME = "Destination.findByName";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String name;

	public Destination() {
		super();
	}

	public Destination(String name) {
		super();
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
