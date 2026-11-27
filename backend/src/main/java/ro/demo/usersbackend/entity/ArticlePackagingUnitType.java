package ro.demo.usersbackend.entity;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
@NamedQueries({ //
		@NamedQuery(name = "ArticlePackagingUnitType.findByArticleAndPackagingUnitType", //
				query = "from ArticlePackagingUnitType a where a.article = :article and a.packagingUnitType = :pack"), //
		@NamedQuery(name = "ArticlePackagingUnitType.findByBarcode", //
				query = "from ArticlePackagingUnitType a where a.barcode = :barcode"), //
		@NamedQuery(name = "ArticlePackagingUnitType.findByArticleBarcodeIn", //
				query = "from ArticlePackagingUnitType a where a.article.barcode in :barcodes"), //
		@NamedQuery(name = "ArticlePackagingUnitType.findSingleUnitByArticle", //
				query = "from ArticlePackagingUnitType a where a.article = :article and a.packagingUnitType.quantity = 1 order by a.id")//
})
public class ArticlePackagingUnitType {
	public static final String BY_ARTICLE_AND_PACK = "ArticlePackagingUnitType.findByArticleAndPackagingUnitType";
	public static final String BY_BARCODE = "ArticlePackagingUnitType.findByBarcode";
	public static final String BY_ARTICLE_BARCODES = "ArticlePackagingUnitType.findByArticleBarcodeIn";
	public static final String SINGLE_UNIT_BY_ARTICLE = "ArticlePackagingUnitType.findSingleUnitByArticle";

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
