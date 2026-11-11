package ro.demo.usersbackend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

@Entity
public class InventoryOrderLine {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "inventory_order_id", nullable = false)
	private InventoryOrder inventoryOrder;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "load_unit_id")
	private LoadUnit loadUnit;

	@Column(name = "load_unit_barcode", nullable = false, length = 255)
	private String loadUnitBarcode;

	@Column(name = "load_unit_type", length = 50)
	private String loadUnitType;

	@Column(name = "location_name", length = 255)
	private String locationName;

	@Column(name = "completed_at", nullable = false)
	private LocalDateTime completedAt;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "inventory_check_id", unique = true)
	private InventoryCheck inventoryCheck;

	public Long getId() {
		return id;
	}

	public InventoryOrder getInventoryOrder() {
		return inventoryOrder;
	}

	public void setInventoryOrder(InventoryOrder inventoryOrder) {
		this.inventoryOrder = inventoryOrder;
	}

	public LoadUnit getLoadUnit() {
		return loadUnit;
	}

	public void setLoadUnit(LoadUnit loadUnit) {
		this.loadUnit = loadUnit;
	}

	public String getLoadUnitBarcode() {
		return loadUnitBarcode;
	}

	public void setLoadUnitBarcode(String loadUnitBarcode) {
		this.loadUnitBarcode = loadUnitBarcode;
	}

	public String getLoadUnitType() {
		return loadUnitType;
	}

	public void setLoadUnitType(String loadUnitType) {
		this.loadUnitType = loadUnitType;
	}

	public String getLocationName() {
		return locationName;
	}

	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}

	public LocalDateTime getCompletedAt() {
		return completedAt;
	}

	public void setCompletedAt(LocalDateTime completedAt) {
		this.completedAt = completedAt;
	}

	public InventoryCheck getInventoryCheck() {
		return inventoryCheck;
	}

	public void setInventoryCheck(InventoryCheck inventoryCheck) {
		this.inventoryCheck = inventoryCheck;
	}
}
