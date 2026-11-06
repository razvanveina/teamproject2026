package ro.demo.usersbackend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class StockItem {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "articlePackagingUnitType_id", nullable = false)
	private ArticlePackagingUnitType articlePackagingUnitType;

	@Column(nullable = false)
	private int quantity;

	@JsonBackReference
	@ManyToOne
	@JoinColumn(name = "loadUnit_id", nullable = false)
	private LoadUnit loadUnit;

//	@ManyToOne
//	@JoinColumn(name = "packagingUnitType_id", nullable = false)
//	private PackagingUnitType packagingUnitType;

	@ManyToOne
	@JoinColumn(name = "reservation_id")
	private Reservation reservation;

	public StockItem() {
		super();
	}

	public StockItem(ArticlePackagingUnitType articlePackagingUnitType, int quantity, LoadUnit loadUnit) {
		this.setArticlePackagingUnitType(articlePackagingUnitType);
		this.quantity = quantity;
		this.loadUnit = loadUnit;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public int getTotalQuantity() {
		return quantity * articlePackagingUnitType.getPackagingUnitType().getQuantity();
	}

	public LoadUnit getLoadUnit() {
		return loadUnit;
	}

	public void setLoadUnit(LoadUnit loadUnit) {
		this.loadUnit = loadUnit;
	}

//	public PackagingUnitType getPackagingUnitType() {
//		return packagingUnitType;
//	}

//	public void setPackagingUnitType(PackagingUnitType packagingUnitType) {
//		this.packagingUnitType = packagingUnitType;
//	}

	public Reservation getReservation() {
		return reservation;
	}

	public void setReservation(Reservation reservation) {
		this.reservation = reservation;
	}

	public ArticlePackagingUnitType getArticlePackagingUnitType() {
		return articlePackagingUnitType;
	}

	public void setArticlePackagingUnitType(ArticlePackagingUnitType articlePackagingUnitType) {
		this.articlePackagingUnitType = articlePackagingUnitType;
	}
}
