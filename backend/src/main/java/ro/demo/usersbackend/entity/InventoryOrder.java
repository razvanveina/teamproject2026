package ro.demo.usersbackend.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
@NamedQueries({ //
		@NamedQuery(name = "InventoryOrder.findByCodeAndStatus", //
				query = "from InventoryOrder o where o.code = :code and o.status = :status"), //
		@NamedQuery(name = "InventoryOrder.countByCode", //
				query = "select count(o) from InventoryOrder o where o.code = :code")//
})
public class InventoryOrder {
	public static final String BY_CODE_AND_STATUS = "InventoryOrder.findByCodeAndStatus";
	public static final String COUNT_BY_CODE = "InventoryOrder.countByCode";
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "code", nullable = false, unique = true, length = 100)
	private String code;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "finished_at")
	private LocalDateTime finishedAt;

	@Column(name = "reason_message", nullable = false, length = 20)
	private String reasonMessage;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private InventoryOrderStatus status;

	@OneToMany(mappedBy = "inventoryOrder", cascade = CascadeType.ALL)
	private List<InventoryOrderLine> lines = new ArrayList<>();

	public Long getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getFinishedAt() {
		return finishedAt;
	}

	public void setFinishedAt(LocalDateTime finishedAt) {
		this.finishedAt = finishedAt;
	}

	public String getReasonMessage() {
		return reasonMessage;
	}

	public void setReasonMessage(String reasonMessage) {
		this.reasonMessage = reasonMessage;
	}

	public InventoryOrderStatus getStatus() {
		return status;
	}

	public void setStatus(InventoryOrderStatus status) {
		this.status = status;
	}

	public List<InventoryOrderLine> getLines() {
		return lines;
	}

	public void setLines(List<InventoryOrderLine> lines) {
		this.lines = lines;
	}
}
