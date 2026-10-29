package ro.demo.usersbackend.entity;

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
	@JoinColumn(name = "article_id")
	private Article article;

	@Column(nullable = false)
	private int quantity;

	@ManyToOne
	@JoinColumn(name = "loadUnit_id")
	private LoadUnit loadUnit;

	@ManyToOne
	@JoinColumn(name = "packagingUnitType_id")
	private PackagingUnitType packagingUnitType;

	@ManyToOne
	@JoinColumn(name = "reservation_id")
	private Reservation reservation;

	public StockItem() {
		super();
	}

	public Article getArticle() {
		return article;
	}

	public void setArticle(Article article) {
		this.article = article;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public LoadUnit getLoadUnit() {
		return loadUnit;
	}

	public void setLoadUnit(LoadUnit loadUnit) {
		this.loadUnit = loadUnit;
	}

	public PackagingUnitType getPackagingUnitType() {
		return packagingUnitType;
	}

	public void setPackagingUnitType(PackagingUnitType packagingUnitType) {
		this.packagingUnitType = packagingUnitType;
	}

	public Reservation getReservation() {
		return reservation;
	}

	public void setReservation(Reservation reservation) {
		this.reservation = reservation;
	}
}
