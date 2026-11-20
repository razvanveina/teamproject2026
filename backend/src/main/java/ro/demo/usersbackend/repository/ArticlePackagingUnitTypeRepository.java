package ro.demo.usersbackend.repository;

import java.util.List;
import java.util.Optional;

import org.hibernate.Session;

import ro.demo.usersbackend.entity.Article;
import ro.demo.usersbackend.entity.ArticlePackagingUnitType;
import ro.demo.usersbackend.entity.PackagingUnitType;

public class ArticlePackagingUnitTypeRepository extends BaseRepository<ArticlePackagingUnitType, Long> {
	public ArticlePackagingUnitTypeRepository(Session session) {
		super(session, ArticlePackagingUnitType.class);
	}

	public Optional<ArticlePackagingUnitType> findByArticleAndPackagingUnitType(Article article,
			PackagingUnitType pack) {
		return session.createNamedQuery(ArticlePackagingUnitType.BY_ARTICLE_AND_PACK, ArticlePackagingUnitType.class)
				.setParameter("article", article).setParameter("pack", pack).uniqueResultOptional();
	}

	public Optional<ArticlePackagingUnitType> findByBarcode(String barcode) {
		return session.createNamedQuery(ArticlePackagingUnitType.BY_BARCODE, ArticlePackagingUnitType.class)
				.setParameter("barcode", barcode).uniqueResultOptional();
	}

	public List<ArticlePackagingUnitType> findByArticleBarcodeIn(List<String> barcodes) {
		return session.createNamedQuery(ArticlePackagingUnitType.BY_ARTICLE_BARCODES, ArticlePackagingUnitType.class)
				.setParameter("barcodes", barcodes).list();
	}
}