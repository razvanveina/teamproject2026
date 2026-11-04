package ro.demo.usersbackend.server.host;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ro.demo.usersbackend.entity.Article;
import ro.demo.usersbackend.entity.Destination;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.PackagingUnitType;
import ro.demo.usersbackend.entity.Reservation;
import ro.demo.usersbackend.entity.RetrievalOrder;
import ro.demo.usersbackend.entity.RetrievalOrderLine;
import ro.demo.usersbackend.entity.RetrievalOrderLineStatus;
import ro.demo.usersbackend.entity.RetrievalOrderStatus;
import ro.demo.usersbackend.entity.StockItem;
import ro.demo.usersbackend.entity.StorageLocation;
import ro.demo.usersbackend.entity.TransitLocation;
import ro.demo.usersbackend.server.process.AbstractProcess;

public class HostRetrievalProcess extends AbstractProcess {
	private static final Logger Log = LogManager.getLogger(HostRetrievalProcess.class);

	private Path folder;
	private SessionFactory sessionFactory;

	public HostRetrievalProcess() {
		super("hostRetrievalProcess");
	}

	public static void main(String[] args) {
		new HostRetrievalProcess().start();
	}

	@Override
	protected void doAction() {
		try {
			if (sessionFactory == null) {
				folder = Path.of(readFolder());
				sessionFactory = buildSessionFactory();
				Log.info("watching path {}", folder.toAbsolutePath());
			}

			try (Stream<Path> files = Files.list(folder)) {
				files.filter(f -> f.toString().endsWith(".xml")).forEach(f -> process(sessionFactory, f));
			}
		} catch (Exception e) {
			Log.error("cannot scan {}: {}", folder.toAbsolutePath(), e.toString());
		}

	}

	private static String readFolder() throws Exception {
		String value = loadProperties("/config.ini").getProperty("host_retrieval_folder");
		if (value == null) {
			throw new IllegalStateException("host_retrieval_folder is missing in config.ini");
		}
		return value.trim();
	}

	private static Properties loadProperties(String resource) throws Exception {
		Properties properties = new Properties();
		try (InputStream in = HostRetrievalProcess.class.getResourceAsStream(resource)) {
			if (in == null) {
				throw new FileNotFoundException(resource + " not found in project");
			}
			properties.load(in);
		}
		return properties;
	}

	private static void process(SessionFactory sessionFactory, Path file) {
		String name = file.getFileName().toString();
		try {
			Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file.toFile());
			String orderId = value(xml.getDocumentElement(), "id");
			NodeList lines = xml.getElementsByTagName("line");

			boolean created = sessionFactory.fromTransaction(session -> {
				Long existing = session
						.createQuery("select count(o) from RetrievalOrder o where o.orderId = :id", Long.class)
						.setParameter("id", orderId).getSingleResult();
				if (existing > 0) {
					return false;
				}

				RetrievalOrder order = new RetrievalOrder();

				String destinationName = value(xml.getDocumentElement(), "destination");

				Destination destination = session
						.createQuery("from Destination d where d.name = :name", Destination.class)
						.setParameter("name", destinationName).getSingleResult();

				order.setOrderId(orderId);
				order.setStatus(RetrievalOrderStatus.NEW);
				order.setDestination(destination);

				session.persist(order);

				for (int i = 0; i < lines.getLength(); i++) {
					Element line = (Element) lines.item(i);
					String barcode = value(line, "article");

					List<Article> found = session
							.createQuery("from Article a where a.barcode = :barcode", Article.class)
							.setParameter("barcode", barcode).getResultList();

					if (found.isEmpty()) {
						throw new IllegalArgumentException("article " + barcode + " doesn't exist");
					}
					if (found.size() > 1) {
						Log.warn("in {} with barcode {} matches {} articles", name, barcode, found.size());
					}
					Article article;
					if (found.isEmpty()) {
						article = new Article();
						article.setBarcode(barcode);
						article.setDescription(barcode);
						session.persist(article);
					} else {
						article = found.get(0);
					}

					RetrievalOrderLine orderLine = new RetrievalOrderLine();
					orderLine.setOrder(order);
					orderLine.setArticle(article);
					orderLine.setQuantity(Integer.parseInt(value(line, "quantity")));
					orderLine.setStatus(RetrievalOrderLineStatus.NEW);
					session.persist(orderLine);

					List<StockItem> stockItems = session
							.createQuery("from StockItem s where s.article = :article", StockItem.class)
							.setParameter("article", article).getResultList();

					if (stockItems.isEmpty()) {
						throw new IllegalArgumentException("No stock found for article " + barcode);
					}

					Reservation reservation = new Reservation();
					reservation.setStockItem(stockItems.get(0));
					reservation.setRetrievalOrderLine(orderLine);

					session.persist(reservation);

					stockItems.get(0).setReservation(reservation);
				}
				return true;
			});
			if (created) {
				Log.info("in {}, order {} created", name, orderId);
				Files.delete(file);
				Log.info("{} deleted", name);
			} else {
				Log.warn("in {}, order {} already exists", name, orderId);
			}

		} catch (Exception e) {
			Log.error("{} not processsed {}", name, e.toString());
		}
	}

	private static String value(Element parent, String tag) {
		NodeList nodes = parent.getElementsByTagName(tag);
		if (nodes.getLength() == 0) {
			throw new IllegalArgumentException("missing <" + tag + "> element");
		}
		return nodes.item(0).getTextContent().trim();
	}

	private static SessionFactory buildSessionFactory() throws Exception {
		Properties properties = new Properties();
		try (InputStream in = HostRetrievalProcess.class.getResourceAsStream("/application.properties")) {
			properties.load(in);
		}

		StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
				.applySetting("jakarta.persistence.jdbc.url", properties.getProperty("spring.datasource.url"))
				.applySetting("jakarta.persistence.jdbc.user", properties.getProperty("spring.datasource.username"))
				.applySetting("jakarta.persistence.jdbc.password", properties.getProperty("spring.datasource.password"))
				.applySetting("hibernate.physical_naming_strategy",
						"org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy")
				.build();

		return new MetadataSources(registry).addAnnotatedClass(Article.class).addAnnotatedClass(PackagingUnitType.class)
				.addAnnotatedClass(Destination.class).addAnnotatedClass(Reservation.class)
				.addAnnotatedClass(StockItem.class).addAnnotatedClass(LoadUnit.class).addAnnotatedClass(Location.class)
				.addAnnotatedClass(StorageLocation.class).addAnnotatedClass(TransitLocation.class)
				.addAnnotatedClass(RetrievalOrder.class).addAnnotatedClass(RetrievalOrderLine.class).buildMetadata()
				.buildSessionFactory();
	}

}
