package ro.demo.usersbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class InventoryOrderLineItem {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "inventory_order_line_id", nullable = false)
	private InventoryOrderLine inventoryOrderLine;

	@Column(name = "article_barcode", nullable = false, length = 100)
	private String articleBarcode;

	@Column(name = "article_description", length = 255)
	private String articleDescription;

	@Column(name = "packaging_unit_name", nullable = false, length = 255)
	private String packagingUnitName;

	@Column(name = "quantity", nullable = false)
	private int quantity;

	public Long getId() {
		return id;
	}

	public InventoryOrderLine getInventoryOrderLine() {
		return inventoryOrderLine;
	}

	public void setInventoryOrderLine(InventoryOrderLine inventoryOrderLine) {
		this.inventoryOrderLine = inventoryOrderLine;
	}

	public String getArticleBarcode() {
		return articleBarcode;
	}

	public void setArticleBarcode(String articleBarcode) {
		this.articleBarcode = articleBarcode;
	}

	public String getArticleDescription() {
		return articleDescription;
	}

	public void setArticleDescription(String articleDescription) {
		this.articleDescription = articleDescription;
	}

	public String getPackagingUnitName() {
		return packagingUnitName;
	}

	public void setPackagingUnitName(String packagingUnitName) {
		this.packagingUnitName = packagingUnitName;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
}
