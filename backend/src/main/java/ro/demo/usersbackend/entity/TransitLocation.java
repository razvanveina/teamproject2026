package ro.demo.usersbackend.entity;

import java.util.List;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("TRANSIT")
public class TransitLocation extends Location {

	List<LoadUnit> loadUnits;

	public TransitLocation() {
	}
}
