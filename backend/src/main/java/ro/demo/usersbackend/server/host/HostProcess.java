package ro.demo.usersbackend.server.host;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ro.demo.usersbackend.entity.Article;
import ro.demo.usersbackend.entity.PackagingUnitType;
import ro.demo.usersbackend.entity.ReceivingOrder;
import ro.demo.usersbackend.entity.ReceivingOrderLine;
import ro.demo.usersbackend.entity.ReceivingOrderLineStatus;
import ro.demo.usersbackend.entity.ReceivingOrderStatus;

public class HostProcess {
	private static final Logger log = LoggerFactory.getLogger(HostProcess.class);

	public static void main(String[] args) throws Exception {

		((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME))
				.setLevel(ch.qos.logback.classic.Level.INFO);
		SessionFactory sessionFactory = buildSessionFactory();
		Path folder = Path.of("host/demo");
		log.info("watching path {}", folder.toAbsolutePath());

		while (true) {
			try (Stream<Path> files = Files.list(folder)) {
				files.filter(f -> f.toString().endsWith(".xml")).forEach(f -> process(sessionFactory, f));
			} catch (Exception e) {
				log.error("can't read {}, {}", folder.toAbsolutePath(), e.toString());
			}
			Thread.sleep(10000);
		}
	}

	private static void process(SessionFactory sessionFactory, Path file) {
		String name = file.getFileName().toString();
		try {
			Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file.toFile());
			String orderId = value(xml.getDocumentElement(), "id");
			NodeList lines = xml.getElementsByTagName("line");

			boolean created = sessionFactory.fromTransaction(session -> {
				Long existing = session
						.createQuery("select count(o) from ReceivingOrder o where o.orderId = :id", Long.class)
						.setParameter("id", orderId).getSingleResult();
				if (existing > 0) {
					return false;
				}

				ReceivingOrder order = new ReceivingOrder();
				order.setOrderId(orderId);
				order.setStatus(ReceivingOrderStatus.NEW);
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
						log.warn("in {} with barcode {} matches {} articles", name, barcode, found.size());
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

					ReceivingOrderLine orderLine = new ReceivingOrderLine();
					orderLine.setOrder(order);
					orderLine.setArticle(article);
					orderLine.setQuantity(Integer.parseInt(value(line, "quantity")));
					orderLine.setStatus(ReceivingOrderLineStatus.NEW);
					session.persist(orderLine);
				}
				return true;
			});
			if (created) {
				log.info("in {}, order {} created", name, orderId);
				Files.delete(file);
				log.info("{} deleted", name);
			} else
				log.warn("in {}, order {} already exists", name, orderId);

		} catch (Exception e) {
			log.error("{} not processsed {}", name, e.toString());
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
		try (InputStream in = HostProcess.class.getResourceAsStream("/application.properties")) {
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
				.addAnnotatedClass(ReceivingOrder.class).addAnnotatedClass(ReceivingOrderLine.class).buildMetadata()
				.buildSessionFactory();
	}
}