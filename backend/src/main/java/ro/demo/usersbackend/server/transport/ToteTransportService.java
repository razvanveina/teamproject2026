package ro.demo.usersbackend.server.transport;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import ro.demo.usersbackend.entity.Article;
import ro.demo.usersbackend.entity.ArticlePackagingUnitType;
import ro.demo.usersbackend.entity.ArticlePickingLocation;
import ro.demo.usersbackend.entity.Destination;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.LoadUnitType;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.PackagingUnitType;
import ro.demo.usersbackend.entity.Reservation;
import ro.demo.usersbackend.entity.RetrievalOrder;
import ro.demo.usersbackend.entity.RetrievalOrderLine;
import ro.demo.usersbackend.entity.StockItem;
import ro.demo.usersbackend.entity.StorageLocation;
import ro.demo.usersbackend.entity.TransitLocation;
import ro.demo.usersbackend.entity.TransportOrder;
import ro.demo.usersbackend.entity.TransportOrderStatus;
import ro.demo.usersbackend.server.process.AbstractProcess;

// proces care trimite tote-urile cu marfa rezervata din zona de decanting spre locatia de picking a articolului lor
public class ToteTransportService extends AbstractProcess {
	private static final Logger log = LogManager.getLogger("application");

	private SessionFactory sessionFactory;

	public ToteTransportService() {
		super("toteTransportService");
	}

	@Override
	public void doAction() {
		try {
			if (sessionFactory == null) {
				sessionFactory = buildSessionFactory();
			}
			sessionFactory.inTransaction(this::moveTotes);
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

		if (places.isEmpty()) {
			log.warn("Tote transport: no picking location for article {}, tote {} stays", article.getBarcode(), tote.getBarcode());
			return;
		}

		StorageLocation target = places.get(0).getLocation();
		session.persist(new TransportOrder(tote.getLocation(), target, TransportOrderStatus.NEW, tote));
		log.info("Tote transport: tote {} goes to {}", tote.getBarcode(), target.getName());
	}

	private boolean hasReservation(LoadUnit tote) {
		for (StockItem item : tote.getStockItems()) {
			if (item.getReservation() != null) {
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

	private static SessionFactory buildSessionFactory() throws Exception {
		Properties properties = new Properties();
		try (InputStream in = ToteTransportService.class.getResourceAsStream("/application.properties")) {
			properties.load(in);
		}

		StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
				.applySetting("jakarta.persistence.jdbc.url", properties.getProperty("spring.datasource.url"))
				.applySetting("jakarta.persistence.jdbc.user", properties.getProperty("spring.datasource.username"))
				.applySetting("jakarta.persistence.jdbc.password", properties.getProperty("spring.datasource.password"))
				.applySetting("hibernate.physical_naming_strategy",
						"org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy")
				.build();

		return new MetadataSources(registry).addAnnotatedClass(Article.class)
				.addAnnotatedClass(ArticlePackagingUnitType.class).addAnnotatedClass(PackagingUnitType.class)
				.addAnnotatedClass(StockItem.class).addAnnotatedClass(LoadUnit.class).addAnnotatedClass(Location.class)
				.addAnnotatedClass(StorageLocation.class).addAnnotatedClass(TransitLocation.class)
				.addAnnotatedClass(Reservation.class).addAnnotatedClass(RetrievalOrder.class)
				.addAnnotatedClass(RetrievalOrderLine.class).addAnnotatedClass(Destination.class)
				.addAnnotatedClass(TransportOrder.class).addAnnotatedClass(ArticlePickingLocation.class)
				.buildMetadata().buildSessionFactory();
	}
}
