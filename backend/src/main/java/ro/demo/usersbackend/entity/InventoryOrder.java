package ro.demo.usersbackend.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class InventoryOrder {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "reason_message", nullable = false, length = 20)
	private String reasonMessage;

	@OneToMany(mappedBy = "inventoryOrder", cascade = CascadeType.ALL)
	private List<InventoryOrderLine> lines = new ArrayList<>();

	public Long getId() {
		return id;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public String getReasonMessage() {
		return reasonMessage;
	}

	public void setReasonMessage(String reasonMessage) {
		this.reasonMessage = reasonMessage;
	}

	public List<InventoryOrderLine> getLines() {
		return lines;
	}

	public void setLines(List<InventoryOrderLine> lines) {
		this.lines = lines;
	}
}
