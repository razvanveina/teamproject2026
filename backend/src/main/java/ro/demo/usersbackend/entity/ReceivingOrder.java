package ro.demo.usersbackend.entity;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class ReceivingOrder {
	@Id
	String orderId;

	ReceivingOrderStatus status;

	@OneToMany
	List<ReceivingOrderLine> lines;

	public ReceivingOrder() {
		super();
	}

	public ReceivingOrderStatus getStatus() {
		return status;
	}

	public void setStatus(ReceivingOrderStatus status) {
		this.status = status;
	}

	public List<ReceivingOrderLine> getLines() {
		return lines;
	}

	public void setLines(List<ReceivingOrderLine> lines) {
		this.lines = lines;
	}

}
