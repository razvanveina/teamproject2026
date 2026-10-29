package ro.demo.usersbackend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class DestinationToGate {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	String destination;

	@OneToOne
	@JoinColumn(name = "gate_id")
	Location gate;

	public DestinationToGate() {
		super();
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public Location getGate() {
		return gate;
	}

	public void setGate(Location gate) {
		this.gate = gate;
	}

}
