package ro.demo.usersbackend.init;

import java.util.Optional;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import ro.demo.usersbackend.entity.Article;
import ro.demo.usersbackend.repository.ArticleRepository;

@SpringBootApplication(scanBasePackages = { "ro.demo.usersbackend.repository" })
@EntityScan(basePackages = "ro.demo.usersbackend.entity")
@EnableJpaRepositories(basePackages = "ro.demo.usersbackend.repository")
public class DemoTestDataCreator implements CommandLineRunner {

	@PersistenceContext
	private EntityManager entityManager;

	private final ArticleRepository articleRepository;

	public DemoTestDataCreator(ArticleRepository articleRepository) {
		this.articleRepository = articleRepository;
	}

	@Override
	@Transactional
	public void run(String... args) {
		addArticle("111", "Mere");
		addArticle("112", "Pere");
	}

	private void addArticle(String barcode, String desc) {
		Optional<Article> article = articleRepository.findByBarcode(barcode);

		if (article.isPresent()) {
			System.out.println("Article " + barcode + " already exists");
		} else {
			articleRepository.save(new Article(barcode, desc));
		}
	}

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(DemoTestDataCreator.class);
		app.setWebApplicationType(WebApplicationType.NONE);
		app.run(args);
	}
}