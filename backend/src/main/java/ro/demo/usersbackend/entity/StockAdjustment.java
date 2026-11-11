package ro.demo.usersbackend.entity;

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

@Entity
public class StockAdjustment {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "inventory_check_id", nullable = false)
	private InventoryCheck inventoryCheck;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "inventory_order_line_id")
	private InventoryOrderLine inventoryOrderLine;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "article_id")
	private Article article;

	@Column(name = "article_barcode", nullable = false, length = 100)
	private String articleBarcode;

	@Column(name = "article_description", length = 255)
	private String articleDescription;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "article_packaging_unit_type_id")
	private ArticlePackagingUnitType articlePackagingUnitType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "packaging_unit_type_id")
	private PackagingUnitType packagingUnitType;

	@Column(name = "expected_quantity", nullable = false)
	private int expectedQuantity;

	@Column(name = "counted_quantity", nullable = false)
	private int countedQuantity;

	@Column(name = "difference_quantity", nullable = false)
	private int differenceQuantity;

	@Enumerated(EnumType.STRING)
	@Column(name = "reason_message", nullable = false, length = 20)
	private StockAdjustmentReason reasonMessage;

	public InventoryCheck getInventoryCheck() {
		return inventoryCheck;
	}

	public void setInventoryCheck(InventoryCheck inventoryCheck) {
		this.inventoryCheck = inventoryCheck;
	}

	public InventoryOrderLine getInventoryOrderLine() {
		return inventoryOrderLine;
	}

	public void setInventoryOrderLine(InventoryOrderLine inventoryOrderLine) {
		this.inventoryOrderLine = inventoryOrderLine;
	}

	public Article getArticle() {
		return article;
	}

	public void setArticle(Article article) {
		this.article = article;
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

	public ArticlePackagingUnitType getArticlePackagingUnitType() {
		return articlePackagingUnitType;
	}

	public void setArticlePackagingUnitType(ArticlePackagingUnitType articlePackagingUnitType) {
		this.articlePackagingUnitType = articlePackagingUnitType;
	}

	public PackagingUnitType getPackagingUnitType() {
		return packagingUnitType;
	}

	public void setPackagingUnitType(PackagingUnitType packagingUnitType) {
		this.packagingUnitType = packagingUnitType;
	}

	public int getExpectedQuantity() {
		return expectedQuantity;
	}

	public void setExpectedQuantity(int expectedQuantity) {
		this.expectedQuantity = expectedQuantity;
	}

	public int getCountedQuantity() {
		return countedQuantity;
	}

	public void setCountedQuantity(int countedQuantity) {
		this.countedQuantity = countedQuantity;
	}

	public int getDifferenceQuantity() {
		return differenceQuantity;
	}

	public void setDifferenceQuantity(int differenceQuantity) {
		this.differenceQuantity = differenceQuantity;
	}

	public StockAdjustmentReason getReasonMessage() {
		return reasonMessage;
	}

	public void setReasonMessage(StockAdjustmentReason reasonMessage) {
		this.reasonMessage = reasonMessage;
	}
}
