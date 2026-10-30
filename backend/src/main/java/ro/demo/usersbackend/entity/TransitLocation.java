package ro.demo.usersbackend.entity;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

@Entity
@DiscriminatorValue("TRANSIT")
public class TransitLocation extends Location {
	@Column(nullable = false)
	@OneToMany(mappedBy = "location")
	private List<LoadUnit> loadUnits;

	public TransitLocation() {
		super();
	}

	public TransitLocation(String name) {
		setName(name);
	}

	public List<LoadUnit> getLoadUnits() {
		return loadUnits;
	}

	public void setLoadUnits(List<LoadUnit> loadUnits) {
		this.loadUnits = loadUnits;
	}
}
