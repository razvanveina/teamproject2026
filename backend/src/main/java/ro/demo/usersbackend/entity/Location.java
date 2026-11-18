package ro.demo.usersbackend.entity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

	@JsonIgnore
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

	// the load unit on this location (a load unit whose location is this one), or
	// null if it is empty
	@JsonIgnore
	public LoadUnit getLoadUnit() {
		return loadUnits == null || loadUnits.isEmpty() ? null : loadUnits.get(0);
	}

//	public void setLoadUnit(LoadUnit loadUnit) {
//		loadUnits = new ArrayList<LoadUnit>();
//		loadUnits.add(loadUnit);
//	}
}
