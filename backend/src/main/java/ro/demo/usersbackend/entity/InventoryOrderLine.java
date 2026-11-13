package ro.demo.usersbackend.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "location_id", nullable = false)
	private Location location;

	@Column(name = "load_unit_barcode", length = 255)
	private String loadUnitBarcode;

	@Column(name = "load_unit_type", length = 50)
	private String loadUnitType;

	@Column(name = "location_name", length = 255)
	private String locationName;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private InventoryOrderLineStatus status;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	@Column(name = "stock_adjustments_calculated", nullable = false)
	private boolean stockAdjustmentsCalculated;

	@Column(name = "stock_updated", nullable = false)
	private boolean stockUpdated;

	@OneToMany(mappedBy = "inventoryOrderLine", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<InventoryOrderLineItem> items = new ArrayList<>();

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

	public Location getLocation() { return location; }
	public void setLocation(Location location) { this.location = location; }

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

	public InventoryOrderLineStatus getStatus() {
		return status;
	}

	public void setStatus(InventoryOrderLineStatus status) {
		this.status = status;
	}

	public LocalDateTime getCompletedAt() {
		return completedAt;
	}

	public void setCompletedAt(LocalDateTime completedAt) {
		this.completedAt = completedAt;
	}

	public boolean isStockAdjustmentsCalculated() {
		return stockAdjustmentsCalculated;
	}

	public void setStockAdjustmentsCalculated(boolean stockAdjustmentsCalculated) {
		this.stockAdjustmentsCalculated = stockAdjustmentsCalculated;
	}

	public boolean isStockUpdated() {
		return stockUpdated;
	}

	public void setStockUpdated(boolean stockUpdated) {
		this.stockUpdated = stockUpdated;
	}

	public List<InventoryOrderLineItem> getItems() {
		return items;
	}

	public void setItems(List<InventoryOrderLineItem> items) {
		this.items = items;
	}
}
