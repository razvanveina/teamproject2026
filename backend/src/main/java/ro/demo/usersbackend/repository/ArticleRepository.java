package ro.demo.usersbackend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.demo.usersbackend.entity.Article;

public interface ArticleRepository extends JpaRepository<Article, Long> {
	Optional<Article> findByBarcode(String barcode);
}
