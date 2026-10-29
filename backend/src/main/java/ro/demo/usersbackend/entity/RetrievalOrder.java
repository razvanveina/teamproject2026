package ro.demo.usersbackend.entity;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

@Entity
public class RetrievalOrder {
	@Id
	String orderId;

	RetrievalOrderStatus status;

	@OneToMany(mappedBy = "") // TODO
	List<RetrievalOrderLine> lines;

	@OneToOne
	@JoinColumn(name = "destination_id")
	Destination destination;

	public RetrievalOrder() {
		super();
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
