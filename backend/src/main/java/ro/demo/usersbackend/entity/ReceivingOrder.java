package ro.demo.usersbackend.entity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "RECEIVING_ORDER")
public class ReceivingOrder {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String orderId;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private ReceivingOrderStatus status;

	@JsonManagedReference
	@OneToMany(mappedBy = "order")
	private List<ReceivingOrderLine> lines;

	public ReceivingOrder() {
		super();
	}

	public ReceivingOrder(String orderId, ReceivingOrderStatus status) {
		super();
		this.orderId = orderId;
		this.status = status;
	}

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
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
