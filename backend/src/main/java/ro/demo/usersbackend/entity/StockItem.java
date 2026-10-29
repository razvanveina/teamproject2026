package ro.demo.usersbackend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

@Entity
public class StockItem {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne
	@JoinColumn(name = "article_id")
	Article article;

	int quantity;

	@ManyToOne
	@JoinColumn(name = "load_unit_id")
	LoadUnit loadUnit;

	@OneToOne
	@JoinColumn(name = "packaging_unit_type_id")
	PackagingUnitType packagingUnitType;

	@OneToOne
	@JoinColumn(name = "reservation_id")
	Reservation reservation;

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
