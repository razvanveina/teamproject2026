package ro.demo.usersbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class TransportOrder {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "source_id")
	private Location source;

	@ManyToOne
	@JoinColumn(name = "target_id")
	private Location target;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private TransportOrderStatus status;

	@ManyToOne
	@JoinColumn(name = "loadUnit_id")
	private LoadUnit loadUnit;

	public TransportOrder() {
		super();
	}

	public Location getSource() {
		return source;
	}

	public void setSource(Location source) {
		this.source = source;
	}

	public Location getTarget() {
		return target;
	}

	public void setTarget(Location target) {
		this.target = target;
	}

	public TransportOrderStatus getStatus() {
		return status;
	}

	public void setStatus(TransportOrderStatus status) {
		this.status = status;
	}

	public LoadUnit getLoadUnit() {
		return loadUnit;
	}

	public void setLoadUnit(LoadUnit loadUnit) {
		this.loadUnit = loadUnit;
	}
}
