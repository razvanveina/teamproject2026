package ro.demo.usersbackend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

@Entity
public class DestinationToGate {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne
	@JoinColumn(name = "destination_id", nullable = false)
	private Destination destination;

	@ManyToOne
	@JoinColumn(name = "gate_id", nullable = false)
	private Location gate;

	public DestinationToGate() {
		super();
	}

	public DestinationToGate(Destination destination, Location gate) {
		super();
		this.destination = destination;
		this.gate = gate;
	}
	
	public Long getId() {
		return id;
	}

	public Destination getDestination() {
		return destination;
	}

	public void setDestination(Destination destination) {
		this.destination = destination;
	}

	public Location getGate() {
		return gate;
	}

	public void setGate(Location gate) {
		this.gate = gate;
	}
}
