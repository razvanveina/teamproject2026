package ro.demo.usersbackend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "LOCATIONS")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "LOCATION_TYPE")
public abstract class Location {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "location_seq")
	@SequenceGenerator(name = "location_seq", sequenceName = "LOCATIONS_SEQ", allocationSize = 1)
	private Long id;

	protected Location() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
