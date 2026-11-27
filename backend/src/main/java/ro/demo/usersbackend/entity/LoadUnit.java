package ro.demo.usersbackend.entity;

import java.util.List;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
@NamedQueries({ //
		@NamedQuery(name = "LoadUnit.findByBarcode", //
				query = "from LoadUnit lu where lu.barcode = :barcode"), //
		@NamedQuery(name = "LoadUnit.findByLocationOrderByIdAsc", //
				query = "from LoadUnit lu where lu.location = :location order by lu.id asc"), //
		@NamedQuery(name = LoadUnit.COUNT_BY_LOCATION_NAME, //
				query = "select count(lu) from LoadUnit lu where lu.location.name = :name"), //
		@NamedQuery(name = LoadUnit.BY_LOCATION_NAME, //
				query = "from LoadUnit lu where lu.location.name = :name") })
public class LoadUnit {
	public static final String BY_BARCODE = "LoadUnit.findByBarcode";
	public static final String BY_LOCATION_ORDER_BY_ID_ASC = "LoadUnit.findByLocationOrderByIdAsc";
	public static final String BY_LOCATION_NAME = "LoadUnit.findByLocationName";
	public static final String COUNT_BY_LOCATION_NAME = "LoadUnit.countByLocationName";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String barcode;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private LoadUnitType type;

	@ManyToOne
	@JoinColumn(name = "location_id", nullable = false)
	private Location location;

	@JsonManagedReference
//	@Column(nullable = false)
	@OneToMany(mappedBy = "loadUnit")
	private List<StockItem> stockItems;

	public LoadUnit() {
		super();
	}

	public String getBarcode() {
		return barcode;
	}

	public Long getId() {
		return id;
	}

	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	public LoadUnitType getType() {
		return type;
	}

	public void setType(LoadUnitType type) {
		this.type = type;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public List<StockItem> getStockItems() {
		return stockItems;
	}

	public void setStockItems(List<StockItem> stockItems) {
		this.stockItems = stockItems;
	}
}
