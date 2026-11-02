package ro.demo.usersbackend.init;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import ro.demo.usersbackend.entity.Article;
import ro.demo.usersbackend.entity.StorageLocation;
import ro.demo.usersbackend.entity.TransitLocation;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.LoadUnitType;
import ro.demo.usersbackend.entity.StockItem;

@Component
public class InitialDataCreator implements CommandLineRunner {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	@Transactional
	public void run(String... args) {
		createStorageLocations();
		createTransitLocations();
		createPickingLocations();
		createArticles();
		createStock();
	}

	private void createStorageLocations() {
		for (int aisle = 1; aisle <= 5; aisle++) {
			for (int x = 1; x <= 200; x++) {
				for (int y = 1; y <= 5; y++) {
					for (int z = 1; z <= 2; z++) {
						StorageLocation location = new StorageLocation(x, y, z, aisle);
						entityManager.persist(location);
					}
				}
			}
		}
	}

	private void createTransitLocations() {
		createTransitLocation("IN");

		for (int gate = 1; gate <= 10; gate++) {
			createTransitLocation(String.format("GATE%02d", gate));
		}
	}

	private void createTransitLocation(String name) {
		TransitLocation location = new TransitLocation(name);
		entityManager.persist(location);
	}

	private void createPickingLocations() {
		for (int x = 1; x <= 200; x++) {
			for (int y = 1; y <= 5; y++) {
				StorageLocation location = new StorageLocation(x, y);
				entityManager.persist(location);
			}
		}
	}

	private void createArticles() {
		for (int i = 1; i <= 25; i++) {
			Article article = new Article();
			article.setBarcode(String.valueOf(123 + i * 1));
			article.setDescription("desc " + (i * 1));
			entityManager.persist(article);
		}
	}

	private void createStock() {
		List<Article> articles = entityManager.createQuery("from Article a order by a.id", Article.class)
				.getResultList();

		for (int i = 0; i < 20; i++) {
			LoadUnit pallet = new LoadUnit();
			pallet.setBarcode(String.valueOf(1000 + i));
			pallet.setType(LoadUnitType.PALLET);
			entityManager.persist(pallet);

			for (int j = 0; j < 2; j++) {
				StockItem item = new StockItem();
				item.setArticle(articles.get((i + j) % articles.size()));
				item.setQuantity(j == 0 ? 10 : 5);
				item.setLoadUnit(pallet);
				entityManager.persist(item);
			}

			StorageLocation location = entityManager
					.createQuery("from StorageLocation l where l.name = :name", StorageLocation.class)
					.setParameter("name", String.format("HBW%02d%03d%02d%02d", 1, i + 1, 1, 1)).getSingleResult();
			location.setLoadUnit(pallet);
		}
	}
}
