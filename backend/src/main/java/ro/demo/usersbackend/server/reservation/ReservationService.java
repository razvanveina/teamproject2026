package ro.demo.usersbackend.server.reservation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import ro.demo.usersbackend.entity.Article;
import ro.demo.usersbackend.entity.ArticlePackagingUnitType;
import ro.demo.usersbackend.entity.Reservation;
import ro.demo.usersbackend.entity.RetrievalOrder;
import ro.demo.usersbackend.entity.RetrievalOrderLine;
import ro.demo.usersbackend.entity.RetrievalOrderLineStatus;
import ro.demo.usersbackend.entity.RetrievalOrderStatus;
import ro.demo.usersbackend.entity.StockItem;
import ro.demo.usersbackend.server.process.AbstractProcess;
import ro.demo.usersbackend.server.session.ServerSessionFactory;

public class ReservationService extends AbstractProcess {
	private static final Logger Log = LogManager.getLogger("application");

	public ReservationService() {
		super("reservationService");
	}

	public static void main(String[] args) {
		new ReservationService().start();
	}

	@Override
	public void doAction() {
		try {
			SessionFactory sessionFactory = ServerSessionFactory.getInstance();

			List<String> orderIds = sessionFactory.fromTransaction(session -> session
					.createQuery("select o.orderId from RetrievalOrder o where o.status = :status", String.class)
					.setParameter("status", RetrievalOrderStatus.NEW).getResultList());

			orderIds.forEach(orderId -> process(sessionFactory, orderId));
		} catch (Exception e) {
			Log.error("cannot scan retrieval orders: {}", e.toString());
		}
	}

	private static void process(SessionFactory sessionFactory, String orderId) {
		try {
			List<String> items = new ArrayList<>();
			boolean reserved = sessionFactory.fromTransaction(session -> {
				RetrievalOrder order = session
						.createQuery("from RetrievalOrder o where o.orderId = :id", RetrievalOrder.class)
						.setParameter("id", orderId).getSingleResult();
				return reserve(session, order, items);
			});
			if (reserved) {
				Log.info("order {} reserved", orderId);
				items.forEach(item -> Log.info(item));
			}
		} catch (Exception e) {
			Log.error("order {} not processed {}", orderId, e.toString());
		}
	}

	private static boolean reserve(Session session, RetrievalOrder order, List<String> items) {
		for (RetrievalOrderLine line : order.getLines()) {
			int available = findAvailableStock(session, line.getArticle()).stream()
					.mapToInt(StockItem::getTotalQuantity).sum();
			if (available < line.getQuantity()) {
				Log.warn("order {} skipped, article {} has {} available, needs {}", order.getOrderId(),
						line.getArticle().getBarcode(), available, line.getQuantity());
				return false;
			}
		}

		for (RetrievalOrderLine line : order.getLines()) {
			reserveLine(session, line, items);
		}
		order.setStatus(RetrievalOrderStatus.ACTIVE);
		return true;
	}

	private static void reserveLine(Session session, RetrievalOrderLine line, List<String> items) {
		int remaining = line.getQuantity();

		for (StockItem item : findReservableStock(session, line)) {
			if (remaining == 0) {
				break;
			}

			int pieces = wholePackagePieces(item, remaining);
			if (pieces == 0) {
				continue;
			}

			reservePieces(session, item, pieces, line, items);
			remaining -= pieces;
		}

		for (StockItem item : findAvailableStock(session, line.getArticle())) {
			if (remaining == 0) {
				break;
			}

			StockItem opened = openPackage(session, item);
			int pieces = Math.min(opened.getTotalQuantity(), remaining);
			reservePieces(session, opened, pieces, line, items);
			remaining -= pieces;
		}

		if (remaining > 0) {
			throw new IllegalStateException("article " + line.getArticle().getBarcode() + " is short by " + remaining);
		}
		line.setStatus(RetrievalOrderLineStatus.ACTIVE);
	}

	private static void reservePieces(Session session, StockItem item, int pieces, RetrievalOrderLine line,
			List<String> items) {
		int packages = pieces / packageSize(item);
		Article article = line.getArticle();
		items.add("reserved " + article.getDescription() + " with id " + article.getBarcode() + " with package type "
				+ item.getArticlePackagingUnitType().getPackagingUnitType().getName() + " and quantity " + pieces);
		if (item.getQuantity() > packages) {
			item.setQuantity(item.getQuantity() - packages);
			StockItem split = new StockItem(item.getArticlePackagingUnitType(), packages, item.getLoadUnit());
			session.persist(split);
			reserveItem(session, split, line);
		} else {
			reserveItem(session, item, line);
		}
	}

	private static StockItem openPackage(Session session, StockItem item) {
		ArticlePackagingUnitType piece = findPiecePackaging(session, item.getArticlePackagingUnitType().getArticle());
		int size = packageSize(item);

		if (item.getQuantity() <= 1) {
			item.setArticlePackagingUnitType(piece);
			item.setQuantity(size);
			return item;
		}

		item.setQuantity(item.getQuantity() - 1);
		StockItem opened = new StockItem(piece, size, item.getLoadUnit());
		session.persist(opened);
		return opened;
	}

	private static ArticlePackagingUnitType findPiecePackaging(Session session, Article article) {
		return session.createQuery("""
				from ArticlePackagingUnitType p
				where p.article = :article and p.packagingUnitType.quantity = 1
				order by p.id
				""", ArticlePackagingUnitType.class).setParameter("article", article).setMaxResults(1)
				.uniqueResultOptional().orElseThrow(
						() -> new IllegalStateException("article " + article.getBarcode() + " has no piece packaging"));
	}

	private static void reserveItem(Session session, StockItem item, RetrievalOrderLine line) {
		Reservation reservation = new Reservation();
		reservation.setStockItem(item);
		reservation.setRetrievalOrderLine(line);
		session.persist(reservation);
		item.setReservation(reservation);
	}

	private static List<StockItem> findReservableStock(Session session, RetrievalOrderLine line) {
		List<StockItem> stock = findAvailableStock(session, line.getArticle());
		if (reservablePieces(stock, line.getQuantity()) == line.getQuantity()) {
			return stock;
		}
		return stock.stream().sorted(Comparator.comparingInt(ReservationService::packageSize).reversed()).toList();
	}

	private static int reservablePieces(List<StockItem> stock, int quantity) {
		int remaining = quantity;
		for (StockItem item : stock) {
			remaining -= wholePackagePieces(item, remaining);
		}
		return quantity - remaining;
	}

	private static int wholePackagePieces(StockItem item, int quantity) {
		int size = packageSize(item);
		return Math.min(item.getTotalQuantity(), quantity) / size * size;
	}

	private static int packageSize(StockItem item) {
		return item.getArticlePackagingUnitType().getPackagingUnitType().getQuantity();
	}

	private static List<StockItem> findAvailableStock(Session session, Article article) {
		return session.createQuery("""
				select s from StockItem s
				join s.loadUnit u
				join u.location l
				join s.articlePackagingUnitType p
				where p.article = :article and s.reservation is null and type(l) = StorageLocation
				order by case when l.name like 'PICK%' then 0 else 1 end, s.id
				""", StockItem.class).setParameter("article", article).getResultList();
	}
}
