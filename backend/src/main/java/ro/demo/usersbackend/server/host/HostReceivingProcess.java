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
import ro.demo.usersbackend.entity.ReceivingOrder;
import ro.demo.usersbackend.entity.ReceivingOrderLine;
import ro.demo.usersbackend.entity.ReceivingOrderLineStatus;
import ro.demo.usersbackend.entity.ReceivingOrderStatus;
import ro.demo.usersbackend.server.process.AbstractProcess;

public class HostReceivingProcess extends AbstractProcess {
	private static final Logger log = LogManager.getLogger(HostReceivingProcess.class);

	private Path folder;

	public HostReceivingProcess() {
		super("hostReceivingProcess");
	}

	public static void main(String[] args) {
		new HostReceivingProcess().start();
	}

	// scan la folder, AbstractProcess o apeleaza o data la 10 sec
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
		String value = loadProperties("/config.ini").getProperty("host_receiving_folder");
		if (value == null) {
			throw new IllegalStateException("host_receiving_folder is missing in config.ini");
		}
		return value.trim();
	}

	private Properties loadProperties(String resource) throws Exception {
		Properties properties = new Properties();
		try (InputStream in = HostReceivingProcess.class.getResourceAsStream(resource)) {
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
					Long existing = session.createNamedQuery(ReceivingOrder.COUNT_BY_ORDER_ID, Long.class)
							.setParameter("id", orderId).getSingleResult();
					if (existing > 0) {
						setReturnValue(false);
						return;
					}

					ReceivingOrder order = new ReceivingOrder();
					order.setOrderId(orderId);
					order.setStatus(ReceivingOrderStatus.NEW);
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

						ReceivingOrderLine orderLine = new ReceivingOrderLine();
						orderLine.setOrder(order);
						orderLine.setArticle(article);
						orderLine.setQuantity(Integer.parseInt(value(line, "quantity")));
						orderLine.setStatus(ReceivingOrderLineStatus.NEW);
						session.persist(orderLine);
						items.add("received " + article.getDescription() + " with id " + article.getBarcode()
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
}