package ro.demo.usersbackend.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public abstract class Location {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	protected int id;

	@Column(nullable = false, unique = true)
	private String name;

	@OneToMany(mappedBy = "location")
	private List<LoadUnit> loadUnits;

	public Location() {
		super();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public List<LoadUnit> getLoadUnits() {
		return loadUnits;
	}

	public void setLoadUnits(List<LoadUnit> loadUnits) {
		this.loadUnits = loadUnits;
	}

	public LoadUnit getLoadUnit() {
		return loadUnits.get(0);
	}

	public void setLoadUnit(LoadUnit loadUnit) {
		loadUnits = new ArrayList<LoadUnit>();
		loadUnits.add(loadUnit);
	}
}
