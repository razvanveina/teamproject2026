package ro.demo.usersbackend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ro.demo.usersbackend.entity.Article;
import ro.demo.usersbackend.entity.ArticlePackagingUnitType;
import ro.demo.usersbackend.entity.Destination;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.LoadUnitType;
import ro.demo.usersbackend.entity.PackagingUnitType;
import ro.demo.usersbackend.entity.RetrievalOrder;
import ro.demo.usersbackend.entity.RetrievalOrderLine;
import ro.demo.usersbackend.entity.RetrievalOrderLineStatus;
import ro.demo.usersbackend.entity.RetrievalOrderStatus;
import ro.demo.usersbackend.entity.StockItem;
import ro.demo.usersbackend.entity.StorageLocation;
import ro.demo.usersbackend.server.reservation.ReservationService;

public class Test1 {
	private static final String PIECE = "TESTPIECE";
	private static final String BAX6 = "TESTBAX6";
	private static final String BAX12 = "TESTBAX12";

	private static final List<Stock> STOCK = List.of(new Stock("TEST1", PIECE, 10), new Stock("TEST2", PIECE, 10),
			new Stock("TEST3", PIECE, 10), new Stock("TEST4", PIECE, 10), new Stock("TEST4", PIECE, 5),
			new Stock("TEST5", PIECE, 10), new Stock("TEST6", PIECE, 10), new Stock("TEST7", PIECE, 10),
			new Stock("TEST8", PIECE, 10), new Stock("TEST9", PIECE, 10), new Stock("TEST10", BAX6, 2),
			new Stock("TEST11", PIECE, 5), new Stock("TEST11", BAX6, 2), new Stock("TEST12", BAX6, 1),
			new Stock("TEST13", BAX6, 2), new Stock("TEST14", BAX12, 1));

	private static SessionFactory sessionFactory;

	private record Line(String barcode, int quantity) {
	}

	private record Stock(String barcode, String packaging, int quantity) {
	}

	@BeforeAll
	static void start() throws Exception {
		sessionFactory = ReservationService.buildSessionFactory();
//		sessionFactory.inTransaction(Test1::cleanup);
//		sessionFactory.inTransaction(Test1::createTestData);
	}

	@AfterAll
	static void end() {
//		sessionFactory.inTransaction(Test1::cleanup);
		sessionFactory.close();
	}

	@BeforeEach
	void resetData() {
		sessionFactory.inTransaction(Test1::cleanup);
		sessionFactory.inTransaction(Test1::createTestData);
	}

	@Test
	void simpleOrder() {
		createOrder("Order1", new Line("TEST1", 1));
		new ReservationService().doAction();

		assertEquals(List.of(new Line("TEST1", 1)), reservedLines("Order1"));
	}

	@Test
	void twoLinesOrder() {
		createOrder("Order2", new Line("TEST2", 5), new Line("TEST4", 6));
		new ReservationService().doAction();

		assertEquals(List.of(new Line("TEST2", 5), new Line("TEST4", 6)), reservedLines("Order2"));
	}

	@Test
	void imposibleOrder() {
		createOrder("Order3", new Line("TEST1", 1000));
		new ReservationService().doAction();

		assertTrue(reservedLines("Order3").isEmpty());
	}

	@Test // redundant but don't want to rewrite all orders
	void sameArticleOrder() {
		createOrder("Order4", new Line("TEST4", 5));
		new ReservationService().doAction();

		assertEquals(List.of(new Line("TEST4", 5)), reservedLines("Order4"));
	}

	@Test
	void pickZonePriorityOrder() {
		createPickingPallet(new Line("TEST1", 10));
		createOrder("Order5", new Line("TEST1", 1));
		new ReservationService().doAction();

		assertEquals(List.of(new Line("TEST1", 1)), reservedLines("Order5"));
		List<String> locations = reservedLocations("Order5");
		assertTrue(locations.stream().allMatch(name -> name.startsWith("PICK")), locations.toString());
	}

	@Test
	void pickAndDepoOrder() {
		createPickingPallet(new Line("TEST1", 10));
		createOrder("Order7", new Line("TEST1", 15));
		new ReservationService().doAction();

		assertEquals(List.of(new Line("TEST1", 15)), reservedLines("Order7"));
		assertEquals(10, reservedQuantity("Order7", "PICK%"));
		assertEquals(5, reservedQuantity("Order7", "HBW%"));
	}

	@Test
	void pickZoneSplitOrder() {
		createPickingPallet(new Line("TEST1", 10));
		createOrder("Order8", new Line("TEST1", 7));
		new ReservationService().doAction();

		assertEquals(List.of(new Line("TEST1", 7)), reservedLines("Order8"));
		assertEquals(7, reservedQuantity("Order8", "PICK%"));
	}

	@Test
	void twoPickOrders() {
		createPickingPallet(new Line("TEST1", 10));
		createOrder("Order9", new Line("TEST1", 4));
		createOrder("Order10", new Line("TEST1", 3));
		new ReservationService().doAction();

		assertEquals(List.of(new Line("TEST1", 4)), reservedLines("Order9"));
		assertEquals(List.of(new Line("TEST1", 3)), reservedLines("Order10"));
		assertEquals(4, reservedQuantity("Order9", "PICK%"));
		assertEquals(3, reservedQuantity("Order10", "PICK%"));
	}

	@Test
	void wholeBoxOrder() {
		createOrder("Order11", new Line("TEST10", 6));
		new ReservationService().doAction();

		assertEquals(List.of(new Line("TEST10", 6)), reservedLines("Order11"));
		assertEquals(6, availableQuantity("TEST10", BAX6));
	}

	@Test
	void twoBoxOrder() {
		createOrder("Order12", new Line("TEST10", 12));
		new ReservationService().doAction();

		assertEquals(List.of(new Line("TEST10", 12)), reservedLines("Order12"));
		assertEquals(0, availableQuantity("TEST10", BAX6));
	}

	@Test
	void partialBoxOrder() {
		createOrder("Order13", new Line("TEST10", 7));
		new ReservationService().doAction();

		assertEquals(List.of(new Line("TEST10", 7)), reservedLines("Order13"));
		assertEquals(6, reservedPackagingQuantity("Order13", BAX6));
		assertEquals(1, reservedPackagingQuantity("Order13", PIECE));
		assertEquals(0, availableQuantity("TEST10", BAX6));
		assertEquals(5, availableQuantity("TEST10", PIECE));
	}

	@Test
	void boxAndPieceOrder() {
		createOrder("Order14", new Line("TEST11", 7));
		new ReservationService().doAction();

		assertEquals(List.of(new Line("TEST11", 7)), reservedLines("Order14"));
		assertEquals(6, availableQuantity("TEST11", BAX6));
		assertEquals(4, availableQuantity("TEST11", PIECE));
	}

	@Test
	void boxPriorityOrder() {
		createOrder("Order15", new Line("TEST11", 6));
		new ReservationService().doAction();

		assertEquals(List.of(new Line("TEST11", 6)), reservedLines("Order15"));
		assertEquals(6, availableQuantity("TEST11", BAX6));
		assertEquals(5, availableQuantity("TEST11", PIECE));
	}

	@Test
	void piecePriorityOrder() {
		createOrder("Order16", new Line("TEST11", 3));
		new ReservationService().doAction();

		assertEquals(List.of(new Line("TEST11", 3)), reservedLines("Order16"));
		assertEquals(12, availableQuantity("TEST11", BAX6));
		assertEquals(2, availableQuantity("TEST11", PIECE));
	}

	@Test
	void openSingleBoxOrder() {
		createOrder("Order17", new Line("TEST12", 1));
		new ReservationService().doAction();

		assertEquals(List.of(new Line("TEST12", 1)), reservedLines("Order17"));
		assertEquals(1, reservedPackagingQuantity("Order17", PIECE));
		assertEquals(0, availableQuantity("TEST12", BAX6));
		assertEquals(5, availableQuantity("TEST12", PIECE));
	}

	@Test
	void articleWithoutPieceOrder() {
		createOrder("Order18", new Line("TEST13", 1));
		new ReservationService().doAction();

		assertTrue(reservedLines("Order18").isEmpty());
		assertEquals(12, availableQuantity("TEST13", BAX6));
	}

	@Test
	void bax12IntoPiecesOrder() {
		createOrder("Order19", new Line("TEST14", 4));
		new ReservationService().doAction();

		assertEquals(List.of(new Line("TEST14", 4)), reservedLines("Order19"));
		assertEquals(4, reservedPackagingQuantity("Order19", PIECE));
		assertEquals(8, availableQuantity("TEST14", PIECE));
		assertEquals(0, availableQuantity("TEST14", BAX6));
		assertEquals(0, availableQuantity("TEST14", BAX12));
	}

	private static void createTestData(Session session) {
		Destination destination = new Destination();
		destination.setName("TEST");
		session.persist(destination);

		PackagingUnitType piece = new PackagingUnitType(PIECE, 1);
		PackagingUnitType bax6 = new PackagingUnitType(BAX6, 6);
		session.persist(piece);
		session.persist(bax6);
		PackagingUnitType bax12 = new PackagingUnitType(BAX12, 12);
		session.persist(bax12);

		STOCK.stream().map(Stock::barcode).distinct().forEach(barcode -> {
			Article article = new Article(barcode, barcode);
			session.persist(article);
			if (!barcode.equals("TEST13")) {
				session.persist(new ArticlePackagingUnitType(barcode, article, piece));
			}
			session.persist(new ArticlePackagingUnitType(barcode + "BAX6", article, bax6));
			session.persist(new ArticlePackagingUnitType(barcode + "BAX12", article, bax12));
		});

		List<StorageLocation> locations = session
				.createQuery("from StorageLocation l where l.loadUnit is null order by l.id", StorageLocation.class)
				.setMaxResults(STOCK.size()).getResultList();

		for (int i = 0; i < STOCK.size(); i++) {
			Stock stock = STOCK.get(i);
			StorageLocation location = locations.get(i);

			LoadUnit pallet = new LoadUnit();
			pallet.setBarcode("TEST" + i);
			pallet.setType(LoadUnitType.PALLET);
			pallet.setLocation(location);
			session.persist(pallet);

			location.setLoadUnit(pallet);
			session.persist(new StockItem(findPackaging(session, stock.barcode(), stock.packaging()), stock.quantity(),
					pallet));
		}
	}

	private static Article findArticle(Session session, String barcode) {
		return session.createQuery("from Article a where a.barcode = :barcode", Article.class)
				.setParameter("barcode", barcode).getSingleResult();
	}

	private static ArticlePackagingUnitType findPackaging(Session session, String barcode, String packaging) {
		return session.createQuery("""
				from ArticlePackagingUnitType p
				where p.article.barcode = :barcode and p.packagingUnitType.name = :packaging
				""", ArticlePackagingUnitType.class).setParameter("barcode", barcode)
				.setParameter("packaging", packaging).getSingleResult();
	}

	private static void createPickingPallet(Line stock) {
		sessionFactory.inTransaction(session -> {
			StorageLocation location = session.createQuery(
					"from StorageLocation l where l.loadUnit is null and l.name like 'PICK%' order by l.id",
					StorageLocation.class).setMaxResults(1).getSingleResult();

			LoadUnit pallet = new LoadUnit();
			pallet.setBarcode("TESTPICK" + stock.barcode());
			pallet.setType(LoadUnitType.PALLET);
			pallet.setLocation(location);
			session.persist(pallet);

			location.setLoadUnit(pallet);
			session.persist(new StockItem(findPackaging(session, stock.barcode(), PIECE), stock.quantity(), pallet));
		});
	}

	private static long reservedPackagingQuantity(String orderId, String packaging) {
		return sessionFactory.fromTransaction(session -> session.createQuery("""
				select coalesce(sum(s.quantity * s.articlePackagingUnitType.packagingUnitType.quantity), 0L)
				from Reservation r
				join r.stockItem s
				where r.retrievalOrderLine.order.orderId = :orderId
				and s.articlePackagingUnitType.packagingUnitType.name = :packaging
				""", Long.class).setParameter("orderId", orderId).setParameter("packaging", packaging)
				.getSingleResult());
	}

	private static long availableQuantity(String barcode, String packaging) {
		return sessionFactory.fromTransaction(session -> session.createQuery("""
				select coalesce(sum(s.quantity * s.articlePackagingUnitType.packagingUnitType.quantity), 0L)
				from StockItem s
				where s.reservation is null
				and s.articlePackagingUnitType.article.barcode = :barcode
				and s.articlePackagingUnitType.packagingUnitType.name = :packaging
				""", Long.class).setParameter("barcode", barcode).setParameter("packaging", packaging)
				.getSingleResult());
	}

	private static void createOrder(String orderId, Line... lines) {
		sessionFactory.inTransaction(session -> {
			RetrievalOrder order = new RetrievalOrder();
			order.setOrderId(orderId);
			order.setStatus(RetrievalOrderStatus.NEW);
			order.setDestination(session.createQuery("from Destination d where d.name = 'TEST'", Destination.class)
					.getSingleResult());
			session.persist(order);

			for (Line orderLine : lines) {
				RetrievalOrderLine line = new RetrievalOrderLine();
				line.setOrder(order);
				line.setArticle(findArticle(session, orderLine.barcode()));
				line.setQuantity(orderLine.quantity());
				line.setStatus(RetrievalOrderLineStatus.NEW);
				session.persist(line);
			}
		});
	}

	private static List<Line> reservedLines(String orderId) {
		return sessionFactory.fromTransaction(session -> session.createQuery("""
				select s.articlePackagingUnitType.article.barcode,
				sum(s.quantity * s.articlePackagingUnitType.packagingUnitType.quantity)
				from Reservation r
				join r.stockItem s
				join r.retrievalOrderLine l
				where l.order.orderId = :orderId
				group by l.id, s.articlePackagingUnitType.article.barcode
				order by l.id
				""", Object[].class).setParameter("orderId", orderId).getResultList()).stream()
				.map(row -> new Line((String) row[0], ((Number) row[1]).intValue())).toList();
	}

	private static long reservedQuantity(String orderId, String locationPattern) {
		return sessionFactory.fromTransaction(session -> session.createQuery("""
				select coalesce(sum(s.quantity * s.articlePackagingUnitType.packagingUnitType.quantity), 0L)
				from Reservation r
				join r.stockItem s
				where r.retrievalOrderLine.order.orderId = :orderId
				and s.loadUnit.location.name like :locationPattern
				""", Long.class).setParameter("orderId", orderId).setParameter("locationPattern", locationPattern)
				.getSingleResult());
	}

	private static List<String> reservedLocations(String orderId) {
		return sessionFactory.fromTransaction(session -> session.createQuery("""
				select s.loadUnit.location.name
				from Reservation r
				join r.stockItem s
				where r.retrievalOrderLine.order.orderId = :orderId
				""", String.class).setParameter("orderId", orderId).getResultList());
	}

	private static void cleanup(Session session) {

		List<Long> reservedItemIds = session
				.createQuery("select s.id from StockItem s where s.reservation is not null order by s.id", Long.class)
				.getResultList();

		session.createMutationQuery("update StockItem s set s.reservation = null where s.reservation is not null")
				.executeUpdate();
		session.createMutationQuery("delete from Reservation").executeUpdate();
		session.createMutationQuery("delete from RetrievalOrderLine").executeUpdate();
		session.createMutationQuery("delete from RetrievalOrder").executeUpdate();

		for (Long id : reservedItemIds) {
			StockItem item = session.find(StockItem.class, id);
			StockItem original = session.createQuery("""
					from StockItem s
					where s.articlePackagingUnitType = :packaging and s.loadUnit = :loadUnit
					order by s.id
					""", StockItem.class).setParameter("packaging", item.getArticlePackagingUnitType())
					.setParameter("loadUnit", item.getLoadUnit()).setMaxResults(1).getSingleResult();

			if (original != item) {
				original.setQuantity(original.getQuantity() + item.getQuantity());
				session.remove(item);
			}
		}

		session.flush();

		List<Long> loadUnitIds = session.createQuery("""
				select u.id from LoadUnit u
				where u.barcode like 'TEST%'
				or (exists (select 1 from StockItem s
						where s.loadUnit = u and s.articlePackagingUnitType.barcode like 'TEST%')
					and not exists (select 1 from StockItem s
						where s.loadUnit = u and s.articlePackagingUnitType.barcode not like 'TEST%'))
				""", Long.class).getResultList();
		List<Long> receivingOrderIds = session.createNativeQuery("""
				select distinct l.order_id from receiving_order_line l
				join article a on a.id = l.article_id
				where a.barcode like 'TEST%'
				and not exists (select 1 from receiving_order_line o
					join article b on b.id = o.article_id
					where o.order_id = l.order_id and b.barcode not like 'TEST%')
				""", Long.class).getResultList();

		session.createMutationQuery("""
				delete from StockItem s
				where s.articlePackagingUnitType.id in
					(select p.id from ArticlePackagingUnitType p where p.barcode like 'TEST%')
				""").executeUpdate();
		if (!loadUnitIds.isEmpty()) {
			session.createMutationQuery("delete from StockItem s where s.loadUnit.id in :ids")
					.setParameter("ids", loadUnitIds).executeUpdate();
			session.createMutationQuery("update StorageLocation l set l.loadUnit = null where l.loadUnit.id in :ids")
					.setParameter("ids", loadUnitIds).executeUpdate();
			session.createNativeMutationQuery("delete from transport_order where load_unit_id in :ids")
					.setParameter("ids", loadUnitIds).executeUpdate();
			session.createMutationQuery("delete from LoadUnit u where u.id in :ids").setParameter("ids", loadUnitIds)
					.executeUpdate();
		}
		session.createNativeMutationQuery("""
				delete from receiving_order_line
				where article_id in (select id from article where barcode like 'TEST%')
				""").executeUpdate();
		if (!receivingOrderIds.isEmpty()) {
			session.createNativeMutationQuery("delete from receiving_order where id in :ids")
					.setParameter("ids", receivingOrderIds).executeUpdate();
		}
		session.createNativeMutationQuery("""
				delete from article_picking_location
				where article_id in (select id from article where barcode like 'TEST%')
				""").executeUpdate();
		session.createNativeMutationQuery("""
				delete from destination_to_gate
				where destination_id in (select id from destination where name = 'TEST')
				""").executeUpdate();
		session.createMutationQuery("delete from ArticlePackagingUnitType p where p.barcode like 'TEST%'")
				.executeUpdate();
		session.createMutationQuery("delete from PackagingUnitType p where p.name like 'TEST%'").executeUpdate();
		session.createMutationQuery("delete from Article a where a.barcode like 'TEST%'").executeUpdate();
		session.createMutationQuery("delete from Destination d where d.name = 'TEST'").executeUpdate();
	}
}
