package ro.demo.usersbackend.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ro.demo.usersbackend.entity.Article;
import ro.demo.usersbackend.repository.ArticleRepository;

@Service
@Transactional
public class ArticleService {
	private final ArticleRepository articleRepository;

	public ArticleService(ArticleRepository articleRepository) {
		super();
		this.articleRepository = articleRepository;
	}

	public Article getArticleById(long id) {
		return articleRepository.findById(id).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cannot find article with the id: " + id));
	}
}
