package ro.demo.usersbackend.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("TRANSIT")
public class TransitLocation extends Location {

	public TransitLocation() {
	}
}
