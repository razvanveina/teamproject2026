package ro.demo.usersbackend.init;

import java.util.List;

import org.hibernate.Session;

import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.entity.Article;
import ro.demo.usersbackend.entity.ArticlePackagingUnitType;
import ro.demo.usersbackend.entity.Destination;
import ro.demo.usersbackend.entity.DestinationToGate;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.LoadUnitType;
import ro.demo.usersbackend.entity.PackagingUnitType;
import ro.demo.usersbackend.entity.Reservation;
import ro.demo.usersbackend.entity.RetrievalOrder;
import ro.demo.usersbackend.entity.RetrievalOrderLine;
import ro.demo.usersbackend.entity.RetrievalOrderLineStatus;
import ro.demo.usersbackend.entity.RetrievalOrderStatus;
import ro.demo.usersbackend.entity.StockItem;
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
		createLoadUnit();
		createSimulatorData();
	}

	private void createSimulatorData() {
		for (String name : List.of("IP1", "IP2", "EMPTY", "DP1", "DP2", "DP3", "TPS1", "TPT1", "PTTS1",
				"PTTT1", "O")) {
			createTransitLocation(name);
		}
	}

	private void createLoadUnit() {
		LoadUnit loadUnit = new LoadUnit();
		loadUnit.setBarcode("123");
		loadUnit.setType(LoadUnitType.CARTON);

		TransitLocation location = new TransitLocation("test");
		loadUnit.setLocation(location);

		Article article = new Article("000", "art");
		PackagingUnitType pkt = new PackagingUnitType("piece", 1);
		ArticlePackagingUnitType type = new ArticlePackagingUnitType("999", article, pkt);
		StockItem stockItem = new StockItem(type, 10, loadUnit);
		RetrievalOrder order = new RetrievalOrder();
		RetrievalOrderLine line = new RetrievalOrderLine();
		Reservation reservation = new Reservation(stockItem, line);
		Destination dest = new Destination("Gate1");
		TransitLocation gate = new TransitLocation("Gate1");
		DestinationToGate dtg = new DestinationToGate(dest, gate);
		stockItem.setReservation(reservation);
		order.setDestination(dest);
		order.setOrderId("00099");
		order.setStatus(RetrievalOrderStatus.FINISHED);
		line.setArticle(article);
		line.setQuantity(1);
		line.setOrder(order);
		line.setStatus(RetrievalOrderLineStatus.FINISHED);

		session.persist(location);
		session.persist(loadUnit);
		session.persist(article);
		session.persist(pkt);
		session.persist(type);
		session.persist(stockItem);
		session.persist(dest);
		session.persist(gate);
		session.persist(dtg);
		session.persist(order);
		session.persist(line);
		session.persist(reservation);
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
