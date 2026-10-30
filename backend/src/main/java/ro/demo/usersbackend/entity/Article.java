package ro.demo.usersbackend.entity;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class Article {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String barcode;

	@Column(nullable = false)
	private String description;

	@OneToMany(mappedBy = "article")
	private List<PackagingUnitType> packagingUnitTypes;

	public Article() {
		super();
	}

	public String getBarcode() {
		return barcode;
	}

	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<PackagingUnitType> getPackagingUnitTypes() {
		return packagingUnitTypes;
	}

	public void setPackagingUnitTypes(List<PackagingUnitType> packagingUnitTypes) {
		this.packagingUnitTypes = packagingUnitTypes;
	}
}
