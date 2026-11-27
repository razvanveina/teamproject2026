package ro.demo.usersbackend.server.host;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.entity.Article;
import ro.demo.usersbackend.entity.Destination;
import ro.demo.usersbackend.entity.RetrievalOrder;
import ro.demo.usersbackend.entity.RetrievalOrderLine;
import ro.demo.usersbackend.entity.RetrievalOrderLineStatus;
import ro.demo.usersbackend.entity.RetrievalOrderStatus;
import ro.demo.usersbackend.repository.RetrievalOrderRepository;
import ro.demo.usersbackend.server.process.AbstractProcess;

public class HostRetrievalProcess extends AbstractProcess {
	private static final Logger log = LogManager.getLogger(HostRetrievalProcess.class);

	private Path folder;

	public HostRetrievalProcess() {
		super("hostRetrievalProcess");
	}

	public static void main(String[] args) {
		new HostRetrievalProcess().start();
	}

	@Override
	protected void doAction() {
		try {
			if (folder == null) {
				folder = Path.of(readFolder());
				log.info("watching path {}", folder.toAbsolutePath());
			}
			try (Stream<Path> files = Files.list(folder)) {
				files.filter(f -> f.toString().endsWith(".xml")).forEach(f -> process(f));
			}
		} catch (Exception e) {
			log.error("cannot scan {}: {}", folder.toAbsolutePath(), e.toString());
		}

	}

	private String readFolder() throws Exception {
		String value = loadProperties("/config.ini").getProperty("host_retrieval_folder");
		if (value == null) {
			throw new IllegalStateException("host_retrieval_folder is missing in config.ini");
		}
		return value.trim();
	}

	private Properties loadProperties(String resource) throws Exception {
		Properties properties = new Properties();
		try (InputStream in = HostRetrievalProcess.class.getResourceAsStream(resource)) {
			if (in == null) {
				throw new FileNotFoundException(resource + " not found in project");
			}
			properties.load(in);
		}
		return properties;
	}

	private void process(Path file) {
		String name = file.getFileName().toString();
		try {
			Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file.toFile());
			String orderId = value(xml.getDocumentElement(), "id");
			NodeList lines = xml.getElementsByTagName("line");
			List<String> items = new ArrayList<>();

			boolean created = new WithSessionAndTransaction<Boolean>() {

				@Override
				protected void executeBusinessLogic(Session session) {
					if (new RetrievalOrderRepository(session).findByOrderId(orderId).isPresent()) {
						setReturnValue(false);
						return;
					}

					RetrievalOrder order = new RetrievalOrder();

					String destinationName = value(xml.getDocumentElement(), "destination");

					Destination destination = session.createNamedQuery(Destination.BY_NAME, Destination.class)
							.setParameter("name", destinationName).getSingleResult();

					order.setOrderId(orderId);
					order.setStatus(RetrievalOrderStatus.NEW);
					order.setDestination(destination);
					order.setAutomatic(isAutomatic(xml.getDocumentElement()));

					session.persist(order);

					for (int i = 0; i < lines.getLength(); i++) {
						Element line = (Element) lines.item(i);
						String barcode = value(line, "article");

						List<Article> found = session.createNamedQuery(Article.BY_BARCODE, Article.class)
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

						RetrievalOrderLine orderLine = new RetrievalOrderLine();
						orderLine.setOrder(order);
						orderLine.setArticle(article);
						orderLine.setQuantity(Integer.parseInt(value(line, "quantity")));
						orderLine.setStatus(RetrievalOrderLineStatus.NEW);
						session.persist(orderLine);
						items.add("requested " + article.getDescription() + " with id " + article.getBarcode()
								+ " and quantity " + orderLine.getQuantity());
					}
					setReturnValue(true);
				}
			}.run();
			if (created) {
				log.info("in {}, order {} created", name, orderId);
				items.forEach(item -> log.info(item));
				Files.delete(file);
				log.info("{} deleted", name);
			} else {
				log.warn("in {}, order {} already exists", name, orderId);
			}

		} catch (Exception e) {
			log.error("{} not processsed {}", name, e.toString());
		}
	}

	private String value(Element parent, String tag) {
		NodeList nodes = parent.getElementsByTagName(tag);
		if (nodes.getLength() == 0) {
			throw new IllegalArgumentException("missing <" + tag + "> element");
		}
		return nodes.item(0).getTextContent().trim();
	}

	// <automatic>true</automatic> din xml daca lipseste, orderul e manual
	private boolean isAutomatic(Element parent) {
		NodeList nodes = parent.getElementsByTagName("automatic");
		return nodes.getLength() > 0 && Boolean.parseBoolean(nodes.item(0).getTextContent().trim());
	}

}