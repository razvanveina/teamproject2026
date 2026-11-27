package ro.demo.usersbackend.entity;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
@NamedQueries({ //
		@NamedQuery(name = "StockItem.findByLoadUnitOrderByIdAsc", //
				query = "from StockItem s where s.loadUnit = :loadUnit order by s.id asc"), //
		@NamedQuery(name = StockItem.COUNT_BY_LOAD_UNIT_BARCODE, //
				query = "select count(s) from StockItem s where s.loadUnit.barcode = :barcode"), //
		@NamedQuery(name = StockItem.COUNT_RESERVED_BY_LOAD_UNIT_BARCODE, //
				query = "select count(s) from StockItem s where s.loadUnit.barcode = :barcode and s.reservation is not null"), //
		@NamedQuery(name = StockItem.COUNT_ON_LOCATION, //
				query = "select count(s) from StockItem s where s.loadUnit.location.name = :location"), //
		@NamedQuery(name = StockItem.COUNT_FOR_ORDER_ON_LOCATION, //
				query = "select count(s) from StockItem s where s.loadUnit.location.name = :location"
						+ " and s.loadUnit.barcode <> :ignore and s.reservation.retrievalOrderLine.order.id = :orderId"),
		@NamedQuery(name = StockItem.FIND_FREE_IN_STORAGE, //
				query = "select s from StockItem s join s.loadUnit u join u.location l"
						+ " where s.reservation is null and type(l) = StorageLocation") })
public class StockItem {

	public static final String FIND_BY_LOADUNIT_ORDER_BY_ID_ASC = "StockItem.findByLoadUnitOrderByIdAsc";
	public static final String COUNT_BY_LOAD_UNIT_BARCODE = "StockItem.countByLoadUnitBarcode";
	public static final String COUNT_RESERVED_BY_LOAD_UNIT_BARCODE = "StockItem.countReservedByLoadUnitBarcode";
	public static final String COUNT_ON_LOCATION = "StockItem.countOnLocation";
	public static final String COUNT_FOR_ORDER_ON_LOCATION = "StockItem.countForOrderOnLocation";
	public static final String FIND_FREE_IN_STORAGE = "StockItem.findFreeInStorage";

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
