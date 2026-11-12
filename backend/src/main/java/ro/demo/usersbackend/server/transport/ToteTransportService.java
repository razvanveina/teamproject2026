package ro.demo.usersbackend.server.transport;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

import ro.demo.usersbackend.entity.Article;
import ro.demo.usersbackend.entity.ArticlePickingLocation;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.LoadUnitType;
import ro.demo.usersbackend.entity.StockItem;
import ro.demo.usersbackend.entity.StorageLocation;
import ro.demo.usersbackend.entity.TransportOrder;
import ro.demo.usersbackend.entity.TransportOrderStatus;
import ro.demo.usersbackend.server.process.AbstractProcess;
import ro.demo.usersbackend.server.session.ServerSessionFactory;

// proces care trimite tote-urile cu marfa rezervata din zona de decanting spre locatia de picking a articolului lor
public class ToteTransportService extends AbstractProcess {
	private static final Logger log = LogManager.getLogger("toteTransport");

	public ToteTransportService() {
		super("toteTransportService");
	}

	@Override
	public void doAction() {
		try {
			ServerSessionFactory.getInstance().inTransaction(this::moveTotes);
		} catch (Exception e) {
			log.error("Tote transport: {}", e.toString());
		}
	}

	// tote-urile din locatiile de decanting; cele fara rezervari raman unde sunt
	private void moveTotes(Session session) {
		List<LoadUnit> totes = session
				.createQuery("from LoadUnit u where u.type = :type and u.location.name like 'DECANTING'", LoadUnit.class)
				.setParameter("type", LoadUnitType.TOTE).getResultList();

		for (LoadUnit tote : totes) {
			if (hasReservation(tote) && !hasOpenTransport(session, tote)) {
				sendToPicking(session, tote);
			}
		}
	}

	// un tote are un singur articol, deci locatia de picking vine din articolul primului stock item
	private void sendToPicking(Session session, LoadUnit tote) {
		Article article = tote.getStockItems().get(0).getArticlePackagingUnitType().getArticle();
		List<ArticlePickingLocation> places = session
				.createQuery("from ArticlePickingLocation a where a.article = :article", ArticlePickingLocation.class)
				.setParameter("article", article).getResultList();

		// daca articolul nu are inca niciun loc de picking, ii dam cel mai apropiat loc liber de inceputul zonei
		StorageLocation target = null;
		StorageLocation from = null;
		if (!places.isEmpty()) {
			target = findFreePlace(session, places);
			from = places.get(0).getLocation();
		}
		if (target == null) {
			target = findNearestFreePlace(session, from);
			if (target == null) {
				log.warn("Tote transport: no free picking location, tote {} stays", tote.getBarcode());
				return;
			}
			session.persist(new ArticlePickingLocation(article, target));
		}

		session.persist(new TransportOrder(tote.getLocation(), target, TransportOrderStatus.NEW, tote));
		log.info("Tote transport: tote {} goes to {}", tote.getBarcode(), target.getName());
	}

	// prima locatie a articolului pe care nu e niciun tote si spre care nu vine niciunul
	private StorageLocation findFreePlace(Session session, List<ArticlePickingLocation> places) {
		for (ArticlePickingLocation place : places) {
			StorageLocation location = place.getLocation();
			if (location.getLoadUnit() == null && !hasTransportTo(session, location)) {
				return location;
			}
		}
		return null;
	}

	// cea mai apropiata locatie de picking libera care nu apartine deja altui articol
	private StorageLocation findNearestFreePlace(Session session, StorageLocation from) {
		List<StorageLocation> free = session
				.createQuery("from StorageLocation l where l.name like 'PICK%' and l.loadUnit is null "
						+ "and l.id not in (select a.location.id from ArticlePickingLocation a)", StorageLocation.class)
				.getResultList();

		StorageLocation nearest = null;
		for (StorageLocation location : free) {
			if (!hasTransportTo(session, location)
					&& (nearest == null || distance(from, location) < distance(from, nearest))) {
				nearest = location;
			}
		}
		return nearest;
	}

	// fara loc de plecare masuram de la inceputul zonei (x = 0, y = 0)
	private int distance(StorageLocation a, StorageLocation b) {
		if (a == null) {
			return b.getX() + b.getY() + b.getZ();
		}
		return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY()) + Math.abs(a.getZ() - b.getZ());
	}

	private boolean hasTransportTo(Session session, StorageLocation location) {
		Long open = session
				.createQuery("select count(t) from TransportOrder t where t.target = :location and t.status in (:new, :active)", Long.class)
				.setParameter("location", location).setParameter("new", TransportOrderStatus.NEW)
				.setParameter("active", TransportOrderStatus.ACTIVE).getSingleResult();
		return open > 0;
	}

	private boolean hasReservation(LoadUnit tote) {
		for (StockItem item : tote.getStockItems()) {
			if (item.getReservation() != null && !item.getReservation().getRetrievalOrderLine().getOrder().isAutomatic()){
				return true;
			}
		}
		return false;
	}

	// nu facem al doilea transport order pentru un tote care are deja unul NEW sau ACTIVE
	private boolean hasOpenTransport(Session session, LoadUnit tote) {
		Long open = session
				.createQuery("select count(t) from TransportOrder t where t.loadUnit = :tote and t.status in (:new, :active)", Long.class)
				.setParameter("tote", tote).setParameter("new", TransportOrderStatus.NEW)
				.setParameter("active", TransportOrderStatus.ACTIVE).getSingleResult();
		return open > 0;
	}
}
