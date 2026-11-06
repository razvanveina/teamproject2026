package ro.demo.usersbackend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.demo.usersbackend.entity.Article;
import ro.demo.usersbackend.entity.ArticlePackagingUnitType;
import ro.demo.usersbackend.entity.PackagingUnitType;

public interface ArticlePackagingUnitTypeRepository extends JpaRepository<ArticlePackagingUnitType, Long> {
	Optional<ArticlePackagingUnitType> findByArticleAndPackagingUnitType(Article article, PackagingUnitType pack);

	Optional<ArticlePackagingUnitType> findByBarcode(String barcode);
	
	List<ArticlePackagingUnitType> findByArticleBarcodeIn(List<String> barcodes);
}
