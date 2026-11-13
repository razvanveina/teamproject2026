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
import ro.demo.usersbackend.entity.ArticlePackagingUnitType;
import ro.demo.usersbackend.entity.PackagingUnitType;
import ro.demo.usersbackend.repository.ArticlePackagingUnitTypeRepository;
import ro.demo.usersbackend.repository.ArticleRepository;
import ro.demo.usersbackend.repository.PackagingUnitTypeRepository;

@SpringBootApplication(scanBasePackages = { "ro.demo.usersbackend.repository" })
@EntityScan(basePackages = "ro.demo.usersbackend.entity")
@EnableJpaRepositories(basePackages = "ro.demo.usersbackend.repository")
public class DemoTestDataCreator implements CommandLineRunner {

	@PersistenceContext
	private EntityManager entityManager;

	private final ArticleRepository articleRepository;
	private final PackagingUnitTypeRepository packagingUnitTypeRepository;
	private final ArticlePackagingUnitTypeRepository articlePackagingUnitTypeRepository;

	public DemoTestDataCreator(ArticleRepository articleRepository,
			PackagingUnitTypeRepository packagingUnitTypeRepository,
			ArticlePackagingUnitTypeRepository articlePackagingUnitTypeRepository) {
		this.articleRepository = articleRepository;
		this.packagingUnitTypeRepository = packagingUnitTypeRepository;
		this.articlePackagingUnitTypeRepository = articlePackagingUnitTypeRepository;
	}

	@Override
	@Transactional
	public void run(String... args) {
		addArticle("111", "Mere");
		addArticle("112", "Pere");

		addPackagingUnitType("PIECE", 1);
		addPackagingUnitType("BAX3", 3);
		addPackagingUnitType("BAX6", 6);
		addPackagingUnitType("BAX12", 12);

		addAP("111", "111", "PIECE");
		addAP("111", "111003", "BAX3");
		addAP("111", "111006", "BAX6");

		addAP("112", "112", "PIECE"); // la piece are acelasi barcode cu pack
		addAP("112", "112003", "BAX3");
		addAP("112", "112012", "BAX12");
	}

	private void addAP(String articleBarcode, String barcode, String packName) {
		Optional<PackagingUnitType> pack = packagingUnitTypeRepository.findByName(packName);
		Optional<Article> article = articleRepository.findByBarcode(articleBarcode);

		if (pack.isPresent() && article.isPresent()) {
			Optional<ArticlePackagingUnitType> ap = articlePackagingUnitTypeRepository
					.findByArticleAndPackagingUnitType(article.get(), pack.get());
			if (ap.isEmpty()) {
				articlePackagingUnitTypeRepository
						.save(new ArticlePackagingUnitType(barcode, article.get(), pack.get()));
			} else {
				System.out.println("Packaging " + packName + " for article " + articleBarcode + " already exists");
			}
		} else {
			System.out.println("Article " + articleBarcode + " or packaging " + packName + " not found");
		}
	}

	private void addPackagingUnitType(String packName, int amount) {
		Optional<PackagingUnitType> pack = packagingUnitTypeRepository.findByName(packName);
		if (!pack.isPresent()) {
			packagingUnitTypeRepository.save(new PackagingUnitType(packName, amount));
		} else {
			System.out.println("Packaging unit type " + packName + " already exists");
		}
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