package ro.demo.usersbackend.entity;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

@Entity
@NamedQueries({ //
		@NamedQuery(name = ArticlePickingLocation.FIND_BY_ARTICLE, //
				query = "from ArticlePickingLocation a where a.article = :article") //
})
public class ArticlePickingLocation {
	public static final String FIND_BY_ARTICLE = "ArticlePickingLocation.findByArticle";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne
	@JoinColumn(name = "article_id", nullable = false)
	private Article article;
	
	@OneToOne
	@JoinColumn(name = "location_id", nullable = false, unique = true)
	private StorageLocation location;
	
	public ArticlePickingLocation(Article article, StorageLocation location) {
		super();
		this.article = article;
		this.location = location;
	}
	
	public ArticlePickingLocation() {
		super();
	}

	public Long getId() {
		return id;
	}

	public Article getArticle() {
		return article;
	}

	public void setArticle(Article article) {
		this.article = article;
	}

	public StorageLocation getLocation() {
		return location;
	}

	public void setLocation(StorageLocation location) {
		this.location = location;
	}
}	
