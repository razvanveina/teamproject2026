package ro.demo.usersbackend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.demo.usersbackend.entity.ArticlePickingLocation;

public interface ArticlePickingLocationRepository extends JpaRepository<ArticlePickingLocation, Long> {
	List<ArticlePickingLocation> findByArticleBarcode(String barcode);
}