package ro.demo.usersbackend.entity;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
@NamedQueries({ //
		@NamedQuery(name = "Article.findByBarcode", //
				query = "from Article a where a.barcode = :barcode"), //
		@NamedQuery(name = "Article.findAll", //
				query = "from Article a order by a.barcode") //
})
public class Article {
	public static final String BY_BARCODE = "Article.findByBarcode";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String barcode;

	@Column(nullable = false)
	private String description;

	public Article() {
		super();
	}

	public Article(String barcode, String description) {
		this.barcode = barcode;
		this.description = description;
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
}
