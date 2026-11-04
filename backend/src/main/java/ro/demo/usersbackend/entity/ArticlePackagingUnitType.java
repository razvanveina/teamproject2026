package ro.demo.usersbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

public class ArticlePackagingUnitType {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String barcode;

	@ManyToOne
	@JoinColumn(name = "article_id", nullable = false)
	private Article article;

	@ManyToOne
	@JoinColumn(name = "packagingUnitType_id", nullable = false)
	private PackagingUnitType packagingUnitType;

	public ArticlePackagingUnitType() {
		super();
	}

	public ArticlePackagingUnitType(String barcode, Article article, PackagingUnitType packagingUnitType) {
		super();
		this.barcode = barcode;
		this.article = article;
		this.packagingUnitType = packagingUnitType;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getBarcode() {
		return barcode;
	}

	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	public Article getArticle() {
		return article;
	}

	public void setArticle(Article article) {
		this.article = article;
	}

	public PackagingUnitType getPackagingUnitType() {
		return packagingUnitType;
	}

	public void setPackagingUnitType(PackagingUnitType packagingUnitType) {
		this.packagingUnitType = packagingUnitType;
	}

}
