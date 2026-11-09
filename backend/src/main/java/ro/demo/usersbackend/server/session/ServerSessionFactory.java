package ro.demo.usersbackend.server.session;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import ro.demo.usersbackend.entity.Article;
import ro.demo.usersbackend.entity.ArticlePackagingUnitType;
import ro.demo.usersbackend.entity.ArticlePickingLocation;
import ro.demo.usersbackend.entity.Destination;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.PackagingUnitType;
import ro.demo.usersbackend.entity.ReceivingOrder;
import ro.demo.usersbackend.entity.ReceivingOrderLine;
import ro.demo.usersbackend.entity.Reservation;
import ro.demo.usersbackend.entity.RetrievalOrder;
import ro.demo.usersbackend.entity.RetrievalOrderLine;
import ro.demo.usersbackend.entity.StockItem;
import ro.demo.usersbackend.entity.StorageLocation;
import ro.demo.usersbackend.entity.TransitLocation;
import ro.demo.usersbackend.entity.TransportOrder;

public final class ServerSessionFactory {
	private static final Logger Log = LogManager.getLogger("serverSession");

	private static SessionFactory sessionFactory;

	private ServerSessionFactory() {
	}

	public static synchronized SessionFactory getInstance() throws Exception {
		if (sessionFactory == null) {
			sessionFactory = build();
			Log.info("session factory built");
		}
		return sessionFactory;
	}

	private static SessionFactory build() throws Exception {
		Properties properties = new Properties();
		try (InputStream in = ServerSessionFactory.class.getResourceAsStream("/application.properties")) {
			if (in == null) {
				throw new FileNotFoundException("/application.properties not found in project");
			}
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
				.addAnnotatedClass(RetrievalOrderLine.class).addAnnotatedClass(ReceivingOrder.class)
				.addAnnotatedClass(ReceivingOrderLine.class).addAnnotatedClass(Destination.class)
				.addAnnotatedClass(TransportOrder.class).addAnnotatedClass(ArticlePickingLocation.class).buildMetadata()
				.buildSessionFactory();
	}
}
