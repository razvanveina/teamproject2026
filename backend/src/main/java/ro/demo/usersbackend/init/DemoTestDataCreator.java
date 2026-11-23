package ro.demo.usersbackend.init;

import java.util.Optional;

import org.hibernate.Session;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.entity.Article;
import ro.demo.usersbackend.entity.ArticlePackagingUnitType;
import ro.demo.usersbackend.entity.PackagingUnitType;
import ro.demo.usersbackend.repository.ArticlePackagingUnitTypeRepository;
import ro.demo.usersbackend.repository.ArticleRepository;
import ro.demo.usersbackend.repository.PackagingUnitTypeRepository;

@SpringBootApplication(scanBasePackages = { "ro.demo.usersbackend.repository" })
@EntityScan(basePackages = "ro.demo.usersbackend.entity")
public class DemoTestDataCreator implements CommandLineRunner {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	@Transactional
	public void run(String... args) {
		new WithSessionAndTransaction<Void>() {
			@Override
			protected void executeBusinessLogic(Session session) {

				ArticleRepository articleRepository = new ArticleRepository(session);
				PackagingUnitTypeRepository packagingUnitTypeRepository = new PackagingUnitTypeRepository(session);
				ArticlePackagingUnitTypeRepository articlePackagingUnitTypeRepository = new ArticlePackagingUnitTypeRepository(
						session);

				addArticle("111", "Mere", articleRepository);
				addArticle("112", "Pere", articleRepository);

				addPackagingUnitType("PIECE", 1, packagingUnitTypeRepository);
				addPackagingUnitType("BAX3", 3, packagingUnitTypeRepository);
				addPackagingUnitType("BAX6", 6, packagingUnitTypeRepository);
				addPackagingUnitType("BAX12", 12, packagingUnitTypeRepository);

				addAP("111", "111", "PIECE", articleRepository, packagingUnitTypeRepository,
						articlePackagingUnitTypeRepository);
				addAP("111", "111003", "BAX3", articleRepository, packagingUnitTypeRepository,
						articlePackagingUnitTypeRepository);
				addAP("111", "111006", "BAX6", articleRepository, packagingUnitTypeRepository,
						articlePackagingUnitTypeRepository);

				addAP("112", "112", "PIECE", articleRepository, packagingUnitTypeRepository,
						articlePackagingUnitTypeRepository); // la piece are acelasi barcode cu pack
				addAP("112", "112003", "BAX3", articleRepository, packagingUnitTypeRepository,
						articlePackagingUnitTypeRepository);
				addAP("112", "112012", "BAX12", articleRepository, packagingUnitTypeRepository,
						articlePackagingUnitTypeRepository);

			}
		}.run();
	}

	private void addAP(String articleBarcode, String barcode, String packName, ArticleRepository articleRepository,
			PackagingUnitTypeRepository packagingUnitTypeRepository,
			ArticlePackagingUnitTypeRepository articlePackagingUnitTypeRepository) {
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

	private void addPackagingUnitType(String packName, int amount,
			PackagingUnitTypeRepository packagingUnitTypeRepository) {
		Optional<PackagingUnitType> pack = packagingUnitTypeRepository.findByName(packName);
		if (!pack.isPresent()) {
			packagingUnitTypeRepository.save(new PackagingUnitType(packName, amount));
		} else {
			System.out.println("Packaging unit type " + packName + " already exists");
		}
	}

	private void addArticle(String barcode, String desc, ArticleRepository articleRepository) {
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