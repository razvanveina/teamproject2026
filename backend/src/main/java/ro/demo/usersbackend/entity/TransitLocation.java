package ro.demo.usersbackend.entity;

import java.util.List;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

@Entity
@DiscriminatorValue("TRANSIT")
public class TransitLocation extends Location {

	@OneToMany(mappedBy = "location")
	List<LoadUnit> loadUnits;

	public TransitLocation() {
	}
}
