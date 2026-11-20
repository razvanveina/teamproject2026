package ro.demo.usersbackend.service;

import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.entity.Article;
import ro.demo.usersbackend.repository.ArticleRepository;

@Service
public class ArticleService {

	public ArticleService() {
		super();
	}

	public Article getArticleById(long id) {
		return new WithSessionAndTransaction<Article>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				ArticleRepository ar = new ArticleRepository(session);
				setReturnValue(ar.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Cannot find article with the id: " + id)));
			}
		}.run();
	}
}
