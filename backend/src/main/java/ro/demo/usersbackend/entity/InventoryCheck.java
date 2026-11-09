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

@Entity
public class InventoryCheck {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "load_unit_id")
	private LoadUnit loadUnit;

	@Column(name = "load_unit_barcode", nullable = false)
	private String loadUnitBarcode;

	@Column(name = "load_unit_type")
	private String loadUnitType;

	@Column(name = "scanned_at", nullable = false)
	private LocalDateTime scannedAt;

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

	public LocalDateTime getScannedAt() {
		return scannedAt;
	}

	public void setScannedAt(LocalDateTime scannedAt) {
		this.scannedAt = scannedAt;
	}
}
