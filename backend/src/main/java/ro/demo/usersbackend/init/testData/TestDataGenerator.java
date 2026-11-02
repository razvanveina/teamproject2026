package ro.demo.usersbackend.init.testData;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import ro.demo.usersbackend.entity.Article;
import ro.demo.usersbackend.entity.ReceivingOrder;
import ro.demo.usersbackend.entity.ReceivingOrderLine;
import ro.demo.usersbackend.entity.ReceivingOrderLineStatus;
import ro.demo.usersbackend.entity.ReceivingOrderStatus;
import ro.demo.usersbackend.service.ArticleService;

@Component
public class TestDataGenerator implements CommandLineRunner {

	@PersistenceContext
	private EntityManager entityManager;

	private final ArticleService articleService;

	public TestDataGenerator(ArticleService articleService) {
		this.articleService = articleService;
	}

	@Override
	@Transactional
	public void run(String... args) {
		createReceivingOrders();
	}

	private void createReceivingOrders() {

		Article art1 = articleService.getArticleById(1);
		Article art2 = articleService.getArticleById(2);

		ReceivingOrder order = new ReceivingOrder("123", ReceivingOrderStatus.NEW);

		ReceivingOrderLine line1 = new ReceivingOrderLine(art1, 10, 10, order, ReceivingOrderLineStatus.NEW);
		ReceivingOrderLine line2 = new ReceivingOrderLine(art2, 15, 0, order, ReceivingOrderLineStatus.NEW);

		entityManager.persist(order);
		entityManager.persist(line1);
		entityManager.persist(line2);
	}
}
