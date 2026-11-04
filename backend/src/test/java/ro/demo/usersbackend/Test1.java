package ro.demo.usersbackend;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ro.demo.usersbackend.entity.Article;
import ro.demo.usersbackend.entity.Destination;
import ro.demo.usersbackend.entity.RetrievalOrder;
import ro.demo.usersbackend.entity.RetrievalOrderLine;
import ro.demo.usersbackend.entity.RetrievalOrderLineStatus;
import ro.demo.usersbackend.entity.RetrievalOrderStatus;
import ro.demo.usersbackend.entity.StockItem;
import ro.demo.usersbackend.server.reservation.ReservationService;

public class Test1 {
	private static SessionFactory sessionFactory;

	private record Line(long articleId, int quantity) {
	}

	@BeforeAll
	static void setUp() throws Exception {
		sessionFactory = ReservationService.buildSessionFactory();
		sessionFactory.inTransaction(Test1::cleanup);
	}

	@AfterAll
	static void tearDown() {
		sessionFactory.inTransaction(Test1::cleanup);
		sessionFactory.close();
	}

	@Test
	void simpleOrder() {

		String total = "select sum(s.quantity) from StockItem s where s.article.id = :articleId";
		String reserved = total + " and s.reservation is not null";

		createOrder("Order1", new Line(1, 1));
		new ReservationService().doAction();

		assertEquals(1, value(reserved, 1));
	}

	@Test
	void twoLinesOrder() {

		String total = "select sum(s.quantity) from StockItem s where s.article.id = :articleId";
		String reserved = total + " and s.reservation is not null";
		createOrder("Order2", new Line(2, 5), new Line(3, 6));
		new ReservationService().doAction();

		assertEquals(5, value(reserved, 2));
		assertEquals(6, value(reserved, 3));
	}

	private static void createOrder(String orderId, Line... lines) {
		sessionFactory.inTransaction(session -> {
			RetrievalOrder order = new RetrievalOrder();
			order.setOrderId(orderId);
			order.setStatus(RetrievalOrderStatus.NEW);
			order.setDestination(
					session.createQuery("from Destination", Destination.class).setMaxResults(1).getSingleResult());
			session.persist(order);

			for (Line orderLine : lines) {
				RetrievalOrderLine line = new RetrievalOrderLine();
				line.setOrder(order);
				line.setArticle(session.find(Article.class, orderLine.articleId()));
				line.setQuantity(orderLine.quantity());
				line.setStatus(RetrievalOrderLineStatus.NEW);
				session.persist(line);
			}
		});
	}

	private static long value(String query, long articleId) {
		return sessionFactory.fromTransaction(session -> session.createQuery(query, Long.class)
				.setParameter("articleId", articleId).getSingleResult());
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
					where s.article = :article and s.loadUnit = :loadUnit and s.packagingUnitType = :packagingUnitType
					order by s.id
					""", StockItem.class).setParameter("article", item.getArticle())
					.setParameter("loadUnit", item.getLoadUnit())
					.setParameter("packagingUnitType", item.getPackagingUnitType()).setMaxResults(1).getSingleResult();

			if (original != item) {
				original.setQuantity(original.getQuantity() + item.getQuantity());
				session.remove(item);
			}
		}
	}
}
