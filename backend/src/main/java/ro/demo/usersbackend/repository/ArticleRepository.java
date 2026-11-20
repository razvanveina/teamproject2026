package ro.demo.usersbackend.repository;

import java.util.Optional;

import org.hibernate.Session;

import ro.demo.usersbackend.entity.Article;

public class ArticleRepository extends BaseRepository<Article, Long> {

	public ArticleRepository(Session session) {
		super(session, Article.class);
	}

	public Optional<Article> findByBarcode(String barcode) {
		return session.createNamedQuery(Article.BY_BARCODE, Article.class).setParameter("barcode", barcode)
				.uniqueResultOptional();
	}
}
