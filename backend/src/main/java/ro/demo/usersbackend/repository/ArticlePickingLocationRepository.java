package ro.demo.usersbackend.repository;

import org.hibernate.Session;

import ro.demo.usersbackend.entity.ArticlePickingLocation;

public class ArticlePickingLocationRepository extends BaseRepository<ArticlePickingLocation, Long> {
	// List<ArticlePickingLocation> findByArticleBarcode(String barcode);

	public ArticlePickingLocationRepository(Session session) {
		super(session, ArticlePickingLocation.class);
	}
}