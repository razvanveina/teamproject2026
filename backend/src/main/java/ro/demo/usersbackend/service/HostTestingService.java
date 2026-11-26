package ro.demo.usersbackend.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.springframework.stereotype.Service;

import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.dto.OrderCheckLine;
import ro.demo.usersbackend.dto.OrderCheckResponse;
import ro.demo.usersbackend.entity.Article;
import ro.demo.usersbackend.entity.Destination;
import ro.demo.usersbackend.entity.Reservation;
import ro.demo.usersbackend.entity.RetrievalOrder;
import ro.demo.usersbackend.entity.RetrievalOrderLine;
import ro.demo.usersbackend.entity.RetrievalOrderStatus;
import ro.demo.usersbackend.entity.StockItem;

@Service
public class HostTestingService {
	public static final String RESERVED = "RESERVED";
	public static final String RESERVING = "RESERVING"; // NEW, dar stocul ajunge: se rezerva la urmatoarea tura
	public static final String CANNOT_BE_FULFILLED = "CANNOT_BE_FULFILLED";

	private final Logger log = LogManager.getLogger("application");
	private final String testOrderIdFormat = "T%04d";
	private final String testOrderIdLikePattern = "T____";
	private final Pattern testOrderIdPattern = Pattern.compile("T(\\d{4})");
	private final int testOrderMaxNumber = 9999;
	private final Random random = new Random();

	public synchronized String createReceivingOrder() throws IOException {
		String orderId = nextTestOrderId("ReceivingOrder.findOrderIdsLike", Constants.HOST_RECEIVING_FOLDER);
		List<String> articleBarcodes = pickRandomArticleBarcodes();
		return writeOrderFile(Constants.HOST_RECEIVING_FOLDER, orderId,
				buildReceivingOrderXml(orderId, articleBarcodes));
	}

	public synchronized String createRetrievalOrder() throws IOException {
		String orderId = nextTestOrderId("RetrievalOrder.findOrderIdsLike", Constants.HOST_RETRIEVAL_FOLDER);
		List<String> articleBarcodes = pickRandomArticleBarcodes();
		String destinationName = pickRandomDestinationName();
		return writeOrderFile(Constants.HOST_RETRIEVAL_FOLDER, orderId,
				buildRetrievalOrderXml(orderId, destinationName, articleBarcodes));
	}

	private String buildReceivingOrderXml(String orderId, List<String> articleBarcodes) {
		return "<order>\n" //
				+ "\t<id>" + orderId + "</id>\n" //
				+ buildOrderLinesXml(articleBarcodes, 20, 60) // un receiving acopera mai multe retrieval-uri
				+ "</order>\n";
	}

	private String buildRetrievalOrderXml(String orderId, String destinationName, List<String> articleBarcodes) {
		return "<order>\n" //
				+ "\t<id>" + orderId + "</id>\n" //
				+ "\t<automatic>true</automatic>\n" //
				+ "\t<destination>" + destinationName + "</destination>\n" //
				+ buildOrderLinesXml(articleBarcodes, 1, 5) //
				+ "</order>\n";
	}

	private String buildOrderLinesXml(List<String> articleBarcodes, int minQuantity, int maxQuantity) {
		StringBuilder linesXml = new StringBuilder("\t<lines>\n");
		for (int lineIndex = 0; lineIndex < articleBarcodes.size(); lineIndex++) {
			int quantity = randomBetween(minQuantity, maxQuantity);
			linesXml.append("\t\t<line>\n") //
					.append("\t\t\t<nr>").append(lineIndex + 1).append("</nr>\n") //
					.append("\t\t\t<article>").append(articleBarcodes.get(lineIndex)).append("</article>\n") //
					.append("\t\t\t<quantity>").append(quantity).append("</quantity>\n") //
					.append("\t\t</line>\n");
		}
		return linesXml.append("\t</lines>\n").toString();
	}

	// articole diferite pe fiecare linie, intre 1 si 3 linii
	private List<String> pickRandomArticleBarcodes() throws IOException {
		List<String> articleBarcodes = new WithSessionAndTransaction<List<String>>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				setReturnValue(session.createNamedQuery("Article.findAll", Article.class).getResultList().stream()
						.map(Article::getBarcode).collect(Collectors.toCollection(ArrayList::new)));
			}
		}.run();
		if (articleBarcodes.isEmpty()) {
			throw new IOException("there are no articles in the database");
		}
		Collections.shuffle(articleBarcodes, random);
		int lineCount = Math.min(randomBetween(1, 3),
				articleBarcodes.size());
		return articleBarcodes.subList(0, lineCount);
	}

	private String pickRandomDestinationName() throws IOException {
		List<Destination> destinations = new WithSessionAndTransaction<List<Destination>>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				setReturnValue(session.createNamedQuery("Destination.findAll", Destination.class).getResultList());
			}
		}.run();
		if (destinations.isEmpty()) {
			throw new IOException("there are no destinations in the database");
		}
		return destinations.get(random.nextInt(destinations.size())).getName();
	}

	private int randomBetween(int minimum, int maximum) {
		return minimum + random.nextInt(maximum - minimum + 1);
	}

	// numarul urmator vine din comenzile deja importate si din fisierele inca neimportate din folder
	private String nextTestOrderId(String orderIdsQueryName, String folderKey) throws IOException {
		int highestUsedNumber = Math.max(highestTestOrderNumberInDatabase(orderIdsQueryName),
				highestTestOrderNumberInFolder(folderKey));
		int nextNumber = highestUsedNumber + 1;
		if (nextNumber > testOrderMaxNumber) {
			throw new IOException("all test order ids up to " + testOrderIdFormat.formatted(testOrderMaxNumber)
					+ " are used");
		}
		return testOrderIdFormat.formatted(nextNumber);
	}

	private int highestTestOrderNumberInDatabase(String orderIdsQueryName) {
		return new WithSessionAndTransaction<Integer>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				List<String> orderIds = session.createNamedQuery(orderIdsQueryName, String.class)
						.setParameter("pattern", testOrderIdLikePattern).getResultList();
				setReturnValue(highestTestOrderNumber(orderIds.stream()));
			}
		}.run();
	}

	private int highestTestOrderNumberInFolder(String folderKey) throws IOException {
		Path folder = Path.of(readFolder(folderKey));
		if (!Files.isDirectory(folder)) {
			return 0;
		}
		try (Stream<Path> files = Files.list(folder)) {
			return highestTestOrderNumber(files.map(file -> withoutExtension(file.getFileName().toString())));
		}
	}

	private int highestTestOrderNumber(Stream<String> orderIds) {
		return orderIds.map(testOrderIdPattern::matcher).filter(Matcher::matches)
				.mapToInt(matcher -> Integer.parseInt(matcher.group(1))).max().orElse(0);
	}

	private String withoutExtension(String fileName) {
		int dotIndex = fileName.lastIndexOf('.');
		return dotIndex < 0 ? fileName : fileName.substring(0, dotIndex);
	}

	// scriem intai .tmp si apoi redenumim, ca procesele host sa nu citeasca un fisier pe jumatate scris
	private String writeOrderFile(String folderKey, String orderId, String xml) throws IOException {
		Path folder = Path.of(readFolder(folderKey));
		Files.createDirectories(folder);
		Path temporaryFile = folder.resolve(orderId + ".tmp");
		Path orderFile = folder.resolve(orderId + ".xml");
		Files.writeString(temporaryFile, xml);
		Files.move(temporaryFile, orderFile, StandardCopyOption.ATOMIC_MOVE);
		log.info("Host testing: created {}", orderFile.toAbsolutePath());
		return orderFile.getFileName().toString();
	}

	private String readFolder(String folderKey) throws IOException {
		Properties properties = new Properties();
		try (InputStream in = HostTestingService.class.getResourceAsStream(Constants.HOST_CONFIG_FILE)) {
			if (in == null) {
				throw new FileNotFoundException(Constants.HOST_CONFIG_FILE + " not found in project");
			}
			properties.load(in);
		}
		String folder = properties.getProperty(folderKey);
		if (folder == null) {
			throw new IOException(folderKey + " is missing in " + Constants.HOST_CONFIG_FILE);
		}
		return folder.trim();
	}

	// orderele neterminate, in ordinea sosirii, cu aceleasi reguli ca ReservationService:
	// tot orderul sau nimic, doar stoc liber din locatii de depozitare, numarat in bucati
	public List<OrderCheckResponse> checkRetrievalOrders() {
		return new WithSessionAndTransaction<List<OrderCheckResponse>>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				Map<Article, Integer> freePieces = new HashMap<>();
				for (StockItem item : session.createNamedQuery("StockItem.findFreeInStorage", StockItem.class)
						.getResultList()) {
					Article article = item.getArticlePackagingUnitType().getArticle();
					freePieces.put(article, freePieces.getOrDefault(article, 0) + item.getTotalQuantity());
				}

				List<OrderCheckResponse> result = new ArrayList<>();
				for (RetrievalOrder order : session
						.createNamedQuery("RetrievalOrder.findNotFinished", RetrievalOrder.class)
						.setParameter("finished", RetrievalOrderStatus.FINISHED).getResultList()) {
					if (order.getStatus() != RetrievalOrderStatus.NEW) {
						result.add(new OrderCheckResponse(order.getOrderId(), order.getStatus().toString(), RESERVED,
								List.of()));
						continue;
					}

					// cate bucati mai trebuie rezervate din fiecare articol
					Map<Article, Integer> neededPieces = new LinkedHashMap<>();
					for (RetrievalOrderLine line : order.getLines()) {
						int reserved = 0;
						for (Reservation reservation : line.getReservations()) {
							reserved = reserved + reservation.getStockItem().getTotalQuantity();
						}
						Article article = line.getArticle();
						neededPieces.put(article,
								neededPieces.getOrDefault(article, 0) + line.getQuantity() - reserved);
					}

					// un rand pentru fiecare articol al orderului, si cate lipsesc (0 daca ajunge stocul)
					List<OrderCheckLine> lines = new ArrayList<>();
					boolean enoughStock = true;
					for (Article article : neededPieces.keySet()) {
						int needed = neededPieces.get(article);
						int free = freePieces.getOrDefault(article, 0);
						int missing = 0;
						if (needed > free) {
							missing = needed - free;
							enoughStock = false;
						}
						lines.add(new OrderCheckLine(article.getBarcode(), article.getDescription(), needed, free,
								missing));
					}

					if (enoughStock) {
						// orderul ia stocul inaintea urmatorului order
						for (Article article : neededPieces.keySet()) {
							freePieces.put(article, freePieces.getOrDefault(article, 0) - neededPieces.get(article));
						}
						result.add(new OrderCheckResponse(order.getOrderId(), order.getStatus().toString(),
								RESERVING, lines));
					} else {
						result.add(new OrderCheckResponse(order.getOrderId(), order.getStatus().toString(),
								CANNOT_BE_FULFILLED, lines));
					}
				}
				setReturnValue(result);
			}
		}.run();
	}
}
