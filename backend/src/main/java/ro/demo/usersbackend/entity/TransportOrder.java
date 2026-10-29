package ro.demo.usersbackend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class TransportOrder {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne
	@JoinColumn(name = "source_location")
	Location source;

	@OneToOne
	@JoinColumn(name = "target_location")
	Location target;

	TransportOrderStatus status;

	@OneToOne
	@JoinColumn(name = "load_unit")
	LoadUnit loadUnit;
}
