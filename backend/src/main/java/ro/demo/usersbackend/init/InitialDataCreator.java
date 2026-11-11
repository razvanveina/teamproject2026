package ro.demo.usersbackend.init;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import ro.demo.usersbackend.entity.Article;
import ro.demo.usersbackend.entity.Destination;
import ro.demo.usersbackend.entity.StorageLocation;
import ro.demo.usersbackend.entity.TransitLocation;

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
		createDestinations();
		createSimulatorData();
//		createArticles();
//		createPackagingUnitTypes();
//		createStock();
	}

	private void createDestinations() {
		String[] destinations = { "Bucharest", "Timisoara", "Brasov", "Iasi", "Arad" };

		for (String name : destinations) {
			Destination destination = new Destination();
			destination.setName(name);
			entityManager.persist(destination);
		}
	}

	private void createStorageLocations() {
		for (int aisle = 1; aisle <= 5; aisle++) {
			for (int x = 1; x <= 100; x++) {
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
		createTransitLocation("OUT");
		createTransitLocation("DECANTING");
		createTransitLocation("PICKING");
		createTransitLocation("CONVEYOR");

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

	private void createSimulatorData() {
		for (String name : List.of("IP1", "IP2", "IP3", "E", "DP1", "DP2", "DP3", "TPS1", "TPT1", "PTTS1", "PTTT1")) {
			createTransitLocation(name);
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

//	private void createPackagingUnitTypes() {
//		List<Article> articles = entityManager.createQuery("from Article a order by a.id", Article.class)
//				.getResultList();
//
//		for (int i = 0; i < articles.size(); i++) {
//			PackagingUnitType type = new PackagingUnitType();
//			type.setName("case " + (i + 1));
//			type.setQuantity(5);
//			entityManager.persist(type);
//
//			Number result = (Number) entityManager
//					.createNativeQuery("SELECT PACKAGINGUNITTYPE_SEQUENCE.NEXTVAL FROM DUAL").getSingleResult();
//			String barcode = String.format("%05d", result.longValue());
//			entityManager.persist(new ArticlePackagingUnitType(barcode, articles.get(i), type));
//		}
//
//	}

//	private void createStock() {
//		List<PackagingUnitType> types = entityManager
//				.createQuery("from PackagingUnitType p order by p.id", PackagingUnitType.class).getResultList();
//
//		for (int i = 0; i < 20; i++) {
//			StorageLocation location = entityManager
//					.createQuery("from StorageLocation l where l.name = :name", StorageLocation.class)
//					.setParameter("name", String.format("HBW%02d%03d%02d%02d", 1, i + 1, 1, 1)).getSingleResult();
//
//			LoadUnit pallet = new LoadUnit();
//			pallet.setBarcode(String.valueOf(1000 + i));
//			pallet.setType(LoadUnitType.PALLET);
//			pallet.setLocation(location);
//			entityManager.persist(pallet);
//
//			location.setLoadUnit(pallet);
//
//			for (int j = 0; j < 2; j++) {
//				PackagingUnitType type = types.get((i + j) % types.size());
//				StockItem item = new StockItem();
//				item.setArticle(type.getArticle());
//				item.setPackagingUnitType(type);
//				item.setQuantity(j == 0 ? 10 : 5);
//				item.setLoadUnit(pallet);
//				entityManager.persist(item);
//			}
//		}
//	}
}