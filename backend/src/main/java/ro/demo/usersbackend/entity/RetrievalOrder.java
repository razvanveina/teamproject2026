package ro.demo.usersbackend.entity;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
public class RetrievalOrder {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String orderId;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private RetrievalOrderStatus status;

	@Column(nullable = false)
	@OneToMany(mappedBy = "order")
	private List<RetrievalOrderLine> lines;

	@ManyToOne
	@JoinColumn(name = "destination_id")
	private Destination destination;

	public RetrievalOrder() {
		super();
	}

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public RetrievalOrderStatus getStatus() {
		return status;
	}

	public void setStatus(RetrievalOrderStatus status) {
		this.status = status;
	}

	public List<RetrievalOrderLine> getLines() {
		return lines;
	}

	public void setLines(List<RetrievalOrderLine> lines) {
		this.lines = lines;
	}

	public Destination getDestination() {
		return destination;
	}

	public void setDestination(Destination destination) {
		this.destination = destination;
	}
}
