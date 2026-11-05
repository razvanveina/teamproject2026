package ro.demo.usersbackend.init;

import org.hibernate.Session;

import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.entity.Article;
import ro.demo.usersbackend.entity.StorageLocation;
import ro.demo.usersbackend.entity.TransitLocation;

public class SimpleInitialDataCreator {
	private Session session;

	public SimpleInitialDataCreator(Session session) {
		super();
		this.session = session;
	}

	public void run() {
		createStorageLocations();
		createTransitLocations();
		createPickingLocations();
		createArticles();
	}

	private void createStorageLocations() {
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

	private void createTransitLocations() {
		createTransitLocation("IN");
		createTransitLocation("OUT");

		for (int gate = 1; gate <= 10; gate++) {
			createTransitLocation(String.format("GATE%02d", gate));
		}
	}

	private void createTransitLocation(String name) {
		TransitLocation location = new TransitLocation(name);
		session.persist(location);
	}

	private void createPickingLocations() {
		for (int x = 1; x <= 200; x++) {
			for (int y = 1; y <= 5; y++) {
				StorageLocation location = new StorageLocation(x, y);
				session.persist(location);
			}
		}
	}

	private void createArticles() {
		for (int i = 1; i <= 25; i++) {
			Article article = new Article();
			article.setBarcode(String.valueOf(123 + i * 1));
			article.setDescription("desc " + (i * 1));
			session.persist(article);
		}
	}

	public static void main(String[] args) {
		new WithSessionAndTransaction<>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				SimpleInitialDataCreator idc = new SimpleInitialDataCreator(session);
				idc.run();
			}
		}.run();

	}
}
