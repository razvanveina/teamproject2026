package ro.demo.usersbackend.init;

import java.util.Optional;

import org.hibernate.Session;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.entity.Area;
import ro.demo.usersbackend.entity.Article;
import ro.demo.usersbackend.entity.ArticlePackagingUnitType;
import ro.demo.usersbackend.entity.Destination;
import ro.demo.usersbackend.entity.DestinationToGate;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.PackagingUnitType;
import ro.demo.usersbackend.entity.Printer;
import ro.demo.usersbackend.entity.PrinterType;
import ro.demo.usersbackend.entity.StorageLocation;
import ro.demo.usersbackend.entity.TransitLocation;
import ro.demo.usersbackend.repository.AreaRepository;
import ro.demo.usersbackend.repository.ArticlePackagingUnitTypeRepository;
import ro.demo.usersbackend.repository.ArticleRepository;
import ro.demo.usersbackend.repository.DestinationToGateRepository;
import ro.demo.usersbackend.repository.LocationRepository;
import ro.demo.usersbackend.repository.PackagingUnitTypeRepository;

@Component
public class InitialDataCreator implements CommandLineRunner {
	// destinatia de pe pozitia i merge la poarta GATE(i + 1)
	private static final String[] DESTINATIONS = { "Bucharest", "Timisoara", "Brasov", "Iasi", "Arad" };

	@Override
	public void run(String... args) {
		new WithSessionAndTransaction<Void>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				if (new LocationRepository(session).count() == 0) {
					createStorageLocations(session);
					createTransitLocations(session);
					createPickingLocations(session);
					createPrinters(session);
				}
				createSimulatorData(session);
				createTransitLocationIfMissing(session, Constants.EMPTY_TOTES, null);
				createDestinations(session);
				createDestinationGates(session);
				createArticles(session);
			}
		}.run();
	}

	private void createPrinters(Session session) {
		session.persist(new Printer("PRINTER1", "192.168.0.100", PrinterType.PDF));
	}

	private void createDestinations(Session session) {
		for (String name : DESTINATIONS) {
			if (findDestination(session, name) == null) {
				session.persist(new Destination(name));
			}
		}
	}

	private void createDestinationGates(Session session) {
		DestinationToGateRepository destinationToGateRepository = new DestinationToGateRepository(session);
		LocationRepository locationRepository = new LocationRepository(session);

		for (int i = 0; i < DESTINATIONS.length; i++) {
			if (destinationToGateRepository.findByDestinationName(DESTINATIONS[i]).isEmpty()) {
				Destination destination = findDestination(session, DESTINATIONS[i]);
				Location gate = locationRepository.findByName(String.format(Constants.GATE_NAME, i + 1)).get();
				session.persist(new DestinationToGate(destination, gate));
			}
		}
	}

	private Destination findDestination(Session session, String name) {
		return session.createNamedQuery("Destination.findByName", Destination.class).setParameter("name", name)
				.uniqueResultOptional().orElse(null);
	}

	private void createArticles(Session session) {
		createArticle(session, "111", "Mere");
		createArticle(session, "112", "Pere");

		createPackagingUnitType(session, "PIECE", 1);
		createPackagingUnitType(session, "BAX3", 3);
		createPackagingUnitType(session, "BAX6", 6);
		createPackagingUnitType(session, "BAX12", 12);

		createArticlePackaging(session, "111", "111", "PIECE");
		createArticlePackaging(session, "111", "111003", "BAX3");
		createArticlePackaging(session, "111", "111006", "BAX6");
		createArticlePackaging(session, "112", "112", "PIECE");
		createArticlePackaging(session, "112", "112003", "BAX3");
		createArticlePackaging(session, "112", "112012", "BAX12");
	}

	private void createArticle(Session session, String barcode, String description) {
		ArticleRepository articleRepository = new ArticleRepository(session);
		if (articleRepository.findByBarcode(barcode).isEmpty()) {
			articleRepository.insert(new Article(barcode, description));
		}
	}

	private void createPackagingUnitType(Session session, String name, int quantity) {
		PackagingUnitTypeRepository packagingUnitTypeRepository = new PackagingUnitTypeRepository(session);
		if (packagingUnitTypeRepository.findByName(name).isEmpty()) {
			packagingUnitTypeRepository.insert(new PackagingUnitType(name, quantity));
		}
	}

	private void createArticlePackaging(Session session, String articleBarcode, String barcode, String packagingName) {
		ArticlePackagingUnitTypeRepository articlePackagingRepository = new ArticlePackagingUnitTypeRepository(session);
		if (articlePackagingRepository.findByBarcode(barcode).isEmpty()) {
			Article article = new ArticleRepository(session).findByBarcode(articleBarcode).get();
			PackagingUnitType packaging = new PackagingUnitTypeRepository(session).findByName(packagingName).get();
			articlePackagingRepository.insert(new ArticlePackagingUnitType(barcode, article, packaging));
		}
	}

	private void createTransitLocationIfMissing(Session session, String name, Area area) {
		if (new LocationRepository(session).findByName(name).isEmpty()) {
			createTransitLocation(session, name, area);
		}
	}

	private void createStorageLocations(Session session) {
		for (int aisle = 1; aisle <= 5; aisle++) {
			for (int x = 1; x <= 100; x++) {
				for (int y = 1; y <= 5; y++) {
					for (int z = 1; z <= 2; z++) {
						StorageLocation location = new StorageLocation(x, y, z, aisle);
						session.persist(location);
					}
				}
			}
		}
	}

	private void createTransitLocations(Session session) {
		createTransitLocation(session, Constants.IN, null);
		createTransitLocation(session, Constants.OUT, null);
		createTransitLocation(session, Constants.DECANTING, null);
		createTransitLocation(session, Constants.PICKING, null);
		createTransitLocation(session, Constants.CONVEYOR, null);

		for (int gate = 1; gate <= 10; gate++) {
			createTransitLocation(session, String.format(Constants.GATE_NAME, gate), null);
		}
	}

	private void createTransitLocation(Session session, String name, Area area) {
		TransitLocation location = new TransitLocation(name, area);
		session.persist(location);
	}

	private void createPickingLocations(Session session) {
		Area pickingArea = getOrCreateArea(session, Constants.PICKING_AREA);

		for (int x = 1; x <= 200; x++) {
			for (int y = 1; y <= 5; y++) {
				StorageLocation location = new StorageLocation(x, y, pickingArea);
				session.persist(location);
			}
		}
	}

	private void createSimulatorData(Session session) {
		Area pickingArea = getOrCreateArea(session, Constants.PICKING_AREA);

		for (String name : Constants.CONVEYOR_POINTS) {
			if (Constants.PICKING_AREA_LOCATIONS.contains(name)) {
				createTransitLocationIfMissing(session, name, pickingArea);
			} else {
				createTransitLocationIfMissing(session, name, null);
			}
		}
	}

	private Area getOrCreateArea(Session session, String name) {
		Area area;
		AreaRepository repo = new AreaRepository(session);
		Optional<Area> areaOpt = repo.findByName(name);

		if (areaOpt.isEmpty()) {
			area = new Area(name);
			session.persist(area);
		} else {
			area = areaOpt.get();
		}
		return area;
	}

//	private void createArticles() {
//		for (int i = 1; i <= 25; i++) {
//			Article article = new Article();
//			article.setBarcode(String.valueOf(123 + i * 1));
//			article.setDescription("desc " + (i * 1));
//			entityManager.persist(article);
//		}
//	}

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