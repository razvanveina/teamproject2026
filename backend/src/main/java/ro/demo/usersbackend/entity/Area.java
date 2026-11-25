package ro.demo.usersbackend.entity;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
@NamedQueries({ //
		@NamedQuery(name = "Area.findByName", //
				query = "select area from Area area where name = :name") })
public class Area {
	public final static String BY_NAME = "Area.findByName";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	private String name;

	public Area() {
		super();
	}

	public Area(String name) {
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
