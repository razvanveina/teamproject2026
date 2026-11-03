package ro.demo.usersbackend.init;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import ro.demo.usersbackend.entity.Article;

@SpringBootApplication(scanBasePackages = { "ro.demo.usersbackend.repository" })
@EntityScan(basePackages = "ro.demo.usersbackend.entity")
public class DemoTestDataCreator implements CommandLineRunner {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	@Transactional
	public void run(String... args) {
		Article article = new Article();
		article.setBarcode("74583489");
		article.setDescription("Mere");
		entityManager.persist(article);
	}

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(DemoTestDataCreator.class);
		app.setWebApplicationType(WebApplicationType.NONE);
		app.run(args);
	}
}