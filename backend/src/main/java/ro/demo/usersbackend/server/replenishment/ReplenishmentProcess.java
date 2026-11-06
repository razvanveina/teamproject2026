package ro.demo.usersbackend.server.replenishment;

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
import ro.demo.usersbackend.entity.Destination;
import ro.demo.usersbackend.entity.LoadUnit;
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

// muta load unit-urile din HBW care au stoc rezervat la DECANTING
public class ReplenishmentProcess extends AbstractProcess {
	private static final Logger Log = LogManager.getLogger(ReplenishmentProcess.class);
	private static final String DECANTING = "DECANTING";
	private static final List<TransportOrderStatus> OPEN = List.of(TransportOrderStatus.NEW,
			TransportOrderStatus.ACTIVE);

	private SessionFactory sessionFactory;

	public ReplenishmentProcess() {
		super("replenishmentProcess");
	}

	public static void main(String[] args) {
		new ReplenishmentProcess().start();
	}

	@Override
	protected void doAction() {
		try {
			if (sessionFactory == null) {
				sessionFactory = buildSessionFactory();
				Log.info("session factory built");
			}

			// load unit-uri din HBW (culoar > 0) cu stoc rezervat si fara transport order deschis
			List<Long> loadUnitIds = sessionFactory.fromTransaction(session -> session.createQuery("""
					select distinct u.id from StorageLocation l
					join l.loadUnit u
					join u.stockItems s
					where l.aisle > 0 and s.reservation is not null
					and not exists (select t.id from TransportOrder t where t.loadUnit = u and t.status in :open)
					""", Long.class).setParameterList("open", OPEN).getResultList());

			loadUnitIds.forEach(this::process);
		} catch (Exception e) {
			Log.error("cannot scan the storage locations: {}", e.toString());
		}
	}

	private void process(Long loadUnitId) {
		try {
			sessionFactory.inTransaction(session -> replenish(session, session.find(LoadUnit.class, loadUnitId)));
		} catch (Exception e) {
			Log.error("load unit {} not processed {}", loadUnitId, e.toString());
		}
	}

	private static void replenish(Session session, LoadUnit loadUnit) {
		Location decanting = session.createQuery("from Location l where l.name = :name", Location.class)
				.setParameter("name", DECANTING).uniqueResultOptional().orElse(null);
		if (decanting == null) {
			Log.error("location {} is missing", DECANTING);
			return;
		}

		session.persist(new TransportOrder(loadUnit.getLocation(), decanting, TransportOrderStatus.NEW, loadUnit));
		Log.info("load unit {} goes from {} to {}", loadUnit.getBarcode(), loadUnit.getLocation().getName(),
				DECANTING);
	}

	private static SessionFactory buildSessionFactory() throws Exception {
		Properties properties = new Properties();
		try (InputStream in = ReplenishmentProcess.class.getResourceAsStream("/application.properties")) {
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
				.addAnnotatedClass(TransportOrder.class).buildMetadata().buildSessionFactory();
	}
}