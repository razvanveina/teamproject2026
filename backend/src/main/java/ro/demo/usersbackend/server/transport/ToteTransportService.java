package ro.demo.usersbackend.server.transport;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.entity.Article;
import ro.demo.usersbackend.entity.ArticlePickingLocation;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.LoadUnitType;
import ro.demo.usersbackend.entity.StorageLocation;
import ro.demo.usersbackend.entity.TransportOrder;
import ro.demo.usersbackend.entity.TransportOrderStatus;
import ro.demo.usersbackend.server.process.AbstractProcess;

// proces care muta tote-urile cu marfa: de la decanting pe banda (automat, prin TPS2), de la TPT1 la locatia de picking
public class ToteTransportService extends AbstractProcess {
	private static final Logger log = LogManager.getLogger("toteTransport");

	public ToteTransportService() {
		super("toteTransportService");
	}

	@Override
	public void doAction() {
//		try {
//			ServerSessionFactory.getInstance().inTransaction(this::moveTotes);
//		} catch (Exception e) {
//			log.error("Tote transport: {}", e.toString());
//		}

		new WithSessionAndTransaction<>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				moveTotes(session);
			}
		}.run();

	}

	private void moveTotes(Session session) {
		List<LoadUnit> totes = session.createNamedQuery("LoadUnit.findByTypeInDecantingOrTpt1", LoadUnit.class)
				.setParameter("type", LoadUnitType.TOTE).setParameterList("places", List.of(Constants.DECANTING))
				.getResultList();

		for (LoadUnit tote : totes) {
			if (tote.getStockItems().isEmpty() || hasOpenTransport(session, tote)) {
				continue;
			}
			sendToPicking(session, tote);
		}
	}

	// un tote are un singur articol, deci locatia de picking vine din articolul
	// primului stock item
	private void sendToPicking(Session session, LoadUnit tote) {
		StorageLocation target = choosePickingPlace(session, tote);
		if (target == null) {
			log.warn("Tote transport: no free picking location, tote {} stays", tote.getBarcode());
			return;
		}

		session.persist(new TransportOrder(tote.getLocation(), target, TransportOrderStatus.NEW, tote));
		log.info("Tote transport: tote {} goes to {}", tote.getBarcode(), target.getName());
	}

	public static StorageLocation choosePickingPlace(Session session, LoadUnit tote) {
		Article article = tote.getStockItems().get(0).getArticlePackagingUnitType().getArticle();
		List<ArticlePickingLocation> places = session
				.createNamedQuery("ArticlePickingLocation.findByArticle", ArticlePickingLocation.class)
				.setParameter("article", article).getResultList();

		// daca articolul nu are inca niciun loc de picking, ii dam cel mai apropiat loc
		// liber de inceputul zonei
		StorageLocation target = null;
		StorageLocation from = null;
		if (!places.isEmpty()) {
			target = findFreePlace(session, places);
			from = places.get(0).getLocation();
		}
		if (target == null) {
			target = findNearestFreePlace(session, from);
			if (target != null) {
				session.persist(new ArticlePickingLocation(article, target));
			}
		}
		return target;
	}

	// prima locatie a articolului pe care nu e niciun tote si spre care nu vine
	// niciunul
	private static StorageLocation findFreePlace(Session session, List<ArticlePickingLocation> places) {
		for (ArticlePickingLocation place : places) {
			StorageLocation location = place.getLocation();
			if (location.getLoadUnit() == null && !hasTransportTo(session, location)) {
				return location;
			}
		}
		return null;
	}

	// cea mai apropiata locatie de picking libera care nu apartine deja altui
	// articol
	private static StorageLocation findNearestFreePlace(Session session, StorageLocation from) {
		List<StorageLocation> free = session
				.createNamedQuery("StorageLocation.findFreePickingLocations", StorageLocation.class)
				.setParameter("pick", Constants.PICK_PREFIX + "%").getResultList();

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
	private static int distance(StorageLocation a, StorageLocation b) {
		if (a == null) {
			return b.getX() + b.getY() + b.getZ();
		}
		return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY()) + Math.abs(a.getZ() - b.getZ());
	}

	private static boolean hasTransportTo(Session session, StorageLocation location) {
		Long open = session.createNamedQuery("TransportOrder.countActiveByTarget", Long.class)
				.setParameter("location", location).setParameterList("open", Constants.OPEN_TRANSPORT_STATUSES)
				.getSingleResult();
		return open > 0;
	}

	// nu facem al doilea transport order pentru un tote care are deja unul NEW sau
	// ACTIVE
	private boolean hasOpenTransport(Session session, LoadUnit tote) {
		Long open = session.createNamedQuery("TransportOrder.countOpenByLoadUnit", Long.class)
				.setParameter("tote", tote).setParameterList("open", Constants.OPEN_TRANSPORT_STATUSES)
				.getSingleResult();
		return open > 0;
	}
}