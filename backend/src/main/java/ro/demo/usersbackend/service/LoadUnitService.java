package ro.demo.usersbackend.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.jms.JMSContext;
import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.dto.LoadUnitResponse;
import ro.demo.usersbackend.dto.ScannedPackage;
import ro.demo.usersbackend.dto.StockItemResponse;
import ro.demo.usersbackend.entity.Article;
import ro.demo.usersbackend.entity.ArticlePackagingUnitType;
import ro.demo.usersbackend.entity.DestinationToGate;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.LoadUnitType;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.PackagingUnitType;
import ro.demo.usersbackend.entity.PrintJob;
import ro.demo.usersbackend.entity.PrintJobStatus;
import ro.demo.usersbackend.entity.RetrievalOrder;
import ro.demo.usersbackend.entity.RetrievalOrderLine;
import ro.demo.usersbackend.entity.RetrievalOrderLineStatus;
import ro.demo.usersbackend.entity.RetrievalOrderStatus;
import ro.demo.usersbackend.entity.StockItem;
import ro.demo.usersbackend.entity.TransportOrder;
import ro.demo.usersbackend.entity.TransportOrderStatus;
import ro.demo.usersbackend.messaging.Broker;
import ro.demo.usersbackend.messaging.MfsMessages;
import ro.demo.usersbackend.repository.ArticlePackagingUnitTypeRepository;
import ro.demo.usersbackend.repository.DestinationToGateRepository;
import ro.demo.usersbackend.repository.LoadUnitRepository;
import ro.demo.usersbackend.repository.LocationRepository;
import ro.demo.usersbackend.repository.RetrievalOrderRepository;
import ro.demo.usersbackend.repository.StockItemRepository;
import ro.demo.usersbackend.repository.TransportOrderRepository;

@Service
public class LoadUnitService {
	private final PrintJobService printJobService;
	private final TransportOrderService transportOrderService;

	public LoadUnitService() {
		super();
		printJobService = new PrintJobService();
		transportOrderService = new TransportOrderService();
	}

	private LoadUnit findLoadUnitByBarcode(Session session, String barcode) {
		return new LoadUnitRepository(session).findByBarcode(barcode)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Cannot find load unit with the barcode: " + barcode));
	}

	private Location findLocationByName(Session session, String name) {
		return new LocationRepository(session).findByName(name)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
						"Location " + name + " is missing"));
	}

	private RetrievalOrder findOrderOfLoadUnit(LoadUnit loadUnit) {
		return loadUnit.getStockItems().stream().filter(s -> s.getReservation() != null)
				.map(s -> s.getReservation().getRetrievalOrderLine().getOrder()).findFirst()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Load unit " + loadUnit.getBarcode() + " does not belong to any order"));
	}

	private void checkLoadUnitIsAt(LoadUnit loadUnit, String targetLocation) {
		String location = loadUnit.getLocation() == null ? "nowhere" : loadUnit.getLocation().getName();
		if (!targetLocation.equals(location)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Load unit " + loadUnit.getBarcode() + " is at "
					+ location + ". Bring it to " + targetLocation + " first");
		}
	}

	private void checkIsCartonOrTote(LoadUnit loadUnit) {
		if (loadUnit.getType() != LoadUnitType.CARTON && loadUnit.getType() != LoadUnitType.TOTE) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Load unit " + loadUnit.getBarcode() + " is a " + loadUnit.getType() + ". Scan a carton or a tote");
		}
	}

	private void checkShippedByUser(RetrievalOrder order, String user, String barcode) {
		if (!user.equals(order.getShippingUser())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					order.getShippingUser() == null ? "Scan the load unit " + barcode + " first"
							: "Load unit " + barcode + " is being shipped by " + order.getShippingUser());
		}
	}

	private boolean areAllOrderLinesFinished(RetrievalOrder order) {
		return order.getLines().stream()
				.allMatch(orderLine -> orderLine.getStatus() == RetrievalOrderLineStatus.FINISHED);
	}

	private Location findGateForOrder(Session session, RetrievalOrder order) {
		DestinationToGate destinationToGate = new DestinationToGateRepository(session)
				.findByDestinationName(order.getDestination().getName())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Could not find a DestinationToGate object from the name: "
								+ order.getDestination().getName()));
		return destinationToGate.getGate();
	}

	// sarim peste barcode-urile folosite deja de alt load unit (create din SQL sau
	// din dialog)
	private String generateLoadUnitBarcode(Session session) {
		LoadUnitRepository loadUnitRepository = new LoadUnitRepository(session);
		String barcode;
		do {
			Long next = session
					.createNativeQuery("SELECT " + Constants.LOAD_UNIT_SEQUENCE + ".NEXTVAL FROM DUAL", Long.class)
					.getSingleResult();
			barcode = String.format(Constants.LOAD_UNIT_BARCODE, next);
		} while (loadUnitRepository.findByBarcode(barcode).isPresent());
		return barcode;
	}

	private String generateCartonBarcode(Session session) {
		LoadUnitRepository loadUnitRepository = new LoadUnitRepository(session);
		String barcode;
		do {
			Long next = session
					.createNativeQuery("SELECT " + Constants.CARTON_SEQUENCE + ".NEXTVAL FROM DUAL", Long.class)
					.getSingleResult();
			barcode = String.format(Constants.CARTON_BARCODE, next);
		} while (loadUnitRepository.findByBarcode(barcode).isPresent());
		return barcode;
	}

	private LoadUnit findOrCreateGeneratedCarton(Session session) {
		for (LoadUnit loadUnit : new LoadUnitRepository(session).findByLocationName(Constants.OUT)) {
			if (loadUnit.getType() == LoadUnitType.CARTON && loadUnit.getStockItems().isEmpty()) {
				return loadUnit;
			}
		}
		return findOrCreateEmptyCarton(session, generateCartonBarcode(session));
	}

	private List<StockItemResponse> toStockItemResponses(List<StockItem> items) {
		List<StockItemResponse> responseItems = new ArrayList<>();

		for (StockItem item : items) {
			ArticlePackagingUnitType aput = item.getArticlePackagingUnitType();
			Article article = aput.getArticle();
			PackagingUnitType unitType = aput.getPackagingUnitType();
			responseItems.add(new StockItemResponse(item.getId(), aput.getBarcode(), item.getQuantity(),
					article.getBarcode(), article.getDescription(), unitType.getName(), unitType.getQuantity()));
		}

		return responseItems;
	}

	private LoadUnit findOrCreateEmptyCarton(Session session, String cartonBarcode) {
		String barcode = cartonBarcode.trim();
		Optional<LoadUnit> existing = new LoadUnitRepository(session).findByBarcode(barcode);

		if (existing.isPresent()) {
			LoadUnit carton = existing.get();
			if (carton.getType() != LoadUnitType.CARTON) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						barcode + " is a " + carton.getType() + ", not a carton");
			}
			if (!carton.getStockItems().isEmpty()) {
				throw new ResponseStatusException(HttpStatus.CONFLICT,
						"Carton " + barcode + " is already used. Take an empty carton");
			}
			carton.setLocation(findLocationByName(session, Constants.OUT));
			return carton;
		}

		LoadUnit carton = new LoadUnit();
		carton.setBarcode(barcode);
		carton.setType(LoadUnitType.CARTON);
		carton.setStockItems(new ArrayList<>());
		carton.setLocation(findLocationByName(session, Constants.OUT));
		new LoadUnitRepository(session).insert(carton);
		return carton;
	}

	private void sendCartonToGateLine(String cartonBarcode) {
		try (JMSContext context = new ActiveMQConnectionFactory(Broker.URL).createContext()) {
			context.createProducer().send(context.createQueue(MfsMessages.FRONTEND_REQUEST_QUEUE), Constants.CREATE_LU
					+ " " + cartonBarcode + " " + Constants.GATE_LINE_START + " " + LoadUnitType.CARTON.name());
		}
	}

	private RetrievalOrder findOrderReadyForLabel(LoadUnit carton, String barcode, String user) {
		if (carton.getType() != LoadUnitType.CARTON) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Load unit " + barcode + " is a " + carton.getType() + ". Only a carton gets a label");
		}
		checkLoadUnitIsAt(carton, Constants.OUT);
		RetrievalOrder order = findOrderOfLoadUnit(carton);
		checkShippedByUser(order, user, barcode);

		if (order.getStatus() != RetrievalOrderStatus.PICKED || !areAllOrderLinesFinished(order)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Confirm the items of carton " + barcode + " first");
		}
		return order;
	}

	// paletul nou de la receiving, cu un stock item pentru fiecare tip de ambalaj
	// scanat
	// primeste sesiunea apelantului, ca ReceivingService sa il poata folosi in
	// tranzactia lui
	public LoadUnit createLoadUnit(Session session, List<ScannedPackage> scannedPackages) {
		Location in = new LocationRepository(session).findByName(Constants.IN).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot find location " + Constants.IN));
		ArticlePackagingUnitTypeRepository packRepository = new ArticlePackagingUnitTypeRepository(session);

		// nr de package uri per tipul de package
		// se aduna daca e acelasi tip de package scanat iar
		Map<ArticlePackagingUnitType, Integer> packagesPerType = new LinkedHashMap<>();
		for (ScannedPackage scanned : scannedPackages) {
			ArticlePackagingUnitType pack = packRepository.findByBarcode(scanned.packageBarcode())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
							"Could not find package with barcode " + scanned.packageBarcode()));

			int total = packagesPerType.getOrDefault(pack, 0);
			packagesPerType.put(pack, total + scanned.packages());
		}

		LoadUnit loadUnit = new LoadUnit();
		loadUnit.setBarcode(generateLoadUnitBarcode(session));
		loadUnit.setType(LoadUnitType.PALLET);
		loadUnit.setLocation(in);
		new LoadUnitRepository(session).insert(loadUnit);

		// un stock item per package type
		// cantitatea e nr de package uri acum
		StockItemRepository stockItemRepository = new StockItemRepository(session);
		for (Map.Entry<ArticlePackagingUnitType, Integer> entry : packagesPerType.entrySet()) {
			stockItemRepository.insert(new StockItem(entry.getKey(), entry.getValue(), loadUnit));
		}
		return loadUnit;
	}

	public LoadUnit createEmptyLoadUnit(String barcode, LoadUnitType type) {
		return new WithSessionAndTransaction<LoadUnit>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				LoadUnitRepository loadUnitRepository = new LoadUnitRepository(session);
				if (loadUnitRepository.findByBarcode(barcode).isPresent()) {
					throw new ResponseStatusException(HttpStatus.CONFLICT,
							"A load unit with the barcode " + barcode + " already exists");
				}

				LoadUnit loadUnit = new LoadUnit();
				loadUnit.setLocation(findLocationByName(session, Constants.CONVEYOR));
				loadUnit.setBarcode(barcode);
				loadUnit.setType(type);
				loadUnitRepository.insert(loadUnit);
				setReturnValue(loadUnit);
			}
		}.run();
	}

	public boolean existsByBarcode(String barcode) {
		return new WithSessionAndTransaction<Boolean>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				setReturnValue(new LoadUnitRepository(session).findByBarcode(barcode).isPresent());
			}
		}.run();
	}

	public Map<String, String> getLabelStatus(String barcode, String user) {
		return new WithSessionAndTransaction<Map<String, String>>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				LoadUnit carton = findLoadUnitByBarcode(session, barcode);
				RetrievalOrder order = findOrderOfLoadUnit(carton);
				checkShippedByUser(order, user, barcode);

				PrintJob latestPrintJob = printJobService.findLatestPrintJobForOrder(order.getOrderId());
				String status = latestPrintJob == null ? Constants.STATUS_MISSING
						: latestPrintJob.getStatus().toString();
				setReturnValue(Map.of("status", status, "label", order.getOrderId()));
			}
		}.run();
	}

	public void releaseLoadUnit(String barcode, String user) {
		new WithSessionAndTransaction<Void>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				LoadUnit loadUnit = findLoadUnitByBarcode(session, barcode);
				RetrievalOrder order = findOrderOfLoadUnit(loadUnit);
				if (user.equals(order.getShippingUser()) && order.getStatus() == RetrievalOrderStatus.PICKED) {
					order.setShippingUser(null);
				}
			}
		}.run();
	}

	// pentru apelantii fara tranzactie proprie; deschide una si foloseste varianta
	// cu sesiune
	public LoadUnit createLoadUnit(List<ScannedPackage> scannedPackages) {
		return new WithSessionAndTransaction<LoadUnit>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				setReturnValue(createLoadUnit(session, scannedPackages));
			}
		}.run();
	}

	public LoadUnitResponse findByBarcode(String barcode, String user) {
		return new WithSessionAndTransaction<LoadUnitResponse>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				LoadUnit loadUnit = findLoadUnitByBarcode(session, barcode);
				checkIsCartonOrTote(loadUnit);
				checkLoadUnitIsAt(loadUnit, Constants.OUT);
				RetrievalOrder order = findOrderOfLoadUnit(loadUnit);

				if (order.getStatus() == RetrievalOrderStatus.FINISHED) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
							"The order for this load unit is already finished");
				}
				if (order.getStatus() != RetrievalOrderStatus.PICKED) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order status must be PICKED");
				}
				if (order.getShippingUser() != null && !order.getShippingUser().equals(user)) {
					throw new ResponseStatusException(HttpStatus.CONFLICT,
							"Load unit " + barcode + " is being shipped by " + order.getShippingUser());
				}

				// userul lucreaza pe un singur order: celelalte ordere PICKED ale lui sunt
				// eliberate
				for (RetrievalOrder other : new RetrievalOrderRepository(session)
						.findByStatusOrderByIdAsc(RetrievalOrderStatus.PICKED)) {
					if (user.equals(other.getShippingUser()) && !other.getId().equals(order.getId())) {
						other.setShippingUser(null);
					}
				}

				order.setShippingUser(user);
				setReturnValue(new LoadUnitResponse(loadUnit.getBarcode(), loadUnit.getType().toString(),
						loadUnit.getLocation().getName(), toStockItemResponses(loadUnit.getStockItems())));
			}
		}.run();
	}

	public LoadUnitResponse chooseCarton(String toteBarcode, String user) {
		return new WithSessionAndTransaction<LoadUnitResponse>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				LoadUnit tote = findLoadUnitByBarcode(session, toteBarcode);
				if (tote.getType() != LoadUnitType.TOTE) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only a tote needs a carton");
				}
				checkLoadUnitIsAt(tote, Constants.OUT);
				RetrievalOrder order = findOrderOfLoadUnit(tote);
				checkShippedByUser(order, user, toteBarcode);

				LoadUnit carton = findOrCreateGeneratedCarton(session);
				setReturnValue(new LoadUnitResponse(carton.getBarcode(), carton.getType().toString(),
						carton.getLocation().getName(), toStockItemResponses(carton.getStockItems())));
			}
		}.run();
	}

	// true = tote automat, controllerul il elibereaza din OUT; run() a facut deja
	// commit cand ajunge acolo
	public boolean finishLoadUnit(String barcode, String user, String cartonBarcode) {
		return new WithSessionAndTransaction<Boolean>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				LoadUnit loadUnit = findLoadUnitByBarcode(session, barcode);
				checkIsCartonOrTote(loadUnit);
				checkLoadUnitIsAt(loadUnit, Constants.OUT);
				RetrievalOrder order = findOrderOfLoadUnit(loadUnit);

				checkShippedByUser(order, user, barcode);

				if (order.getStatus() != RetrievalOrderStatus.PICKED) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order status must be PICKED");
				}

				findGateForOrder(session, order);

				if (areAllOrderLinesFinished(order)) {
					PrintJob latestPrintJob = printJobService.findLatestPrintJobForOrder(order.getOrderId());
					if (latestPrintJob == null || latestPrintJob.getStatus() == PrintJobStatus.ERROR) {
						printJobService.createShippingLabel(order,
								loadUnit.getType() == LoadUnitType.CARTON ? loadUnit.getBarcode() : null);
					}
					setReturnValue(false);
					return;
				}

				String labelCarton = loadUnit.getBarcode(); // un carton scanat direct e chiar cutia etichetei
				if (loadUnit.getType() == LoadUnitType.TOTE) {
					if (cartonBarcode == null || cartonBarcode.isBlank()) {
						throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
								"No carton was assigned to tote " + barcode + ". Scan the tote again");
					}
					LoadUnit carton = findOrCreateEmptyCarton(session, cartonBarcode);
					for (StockItem item : new ArrayList<>(loadUnit.getStockItems())) {
						item.setLoadUnit(carton);
					}
					labelCarton = carton.getBarcode();
				}

				int lineCount = 0;
				for (RetrievalOrderLine line : order.getLines()) {
					if (line.getStatus() == RetrievalOrderLineStatus.PICKED) {
						line.setStatus(RetrievalOrderLineStatus.FINISHED);
					} else {
						lineCount++;
					}
				}

				if (lineCount != 0) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
							"All lines status must be PICKED. There are " + lineCount
									+ " lines with a different status");
				}

				printJobService.createShippingLabel(order, labelCarton);
				if (loadUnit.getType() == LoadUnitType.TOTE) {
					if (order.isAutomatic()) {
						setReturnValue(true); // tote de pe banda, deci controllerul il elibereaza din OUT dupa commit
						return;
					}
					// flux manual: tote-ul gol merge cu stivuitorul la EMPTY_TOTES
					transportOrderService.createTransportOrder(session, loadUnit,
							findLocationByName(session, Constants.EMPTY_TOTES));
				}
				setReturnValue(false);
			}
		}.run();
	}

	public void confirmLabelAndSendCartonToGate(String barcode, String scannedLabel, String user) {
		// true = gate-ul e pe banda, cartonul trebuie pus pe linia de gate-uri dupa
		// commit
		boolean onConveyor = new WithSessionAndTransaction<Boolean>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				LoadUnit carton = findLoadUnitByBarcode(session, barcode);
				RetrievalOrder order = findOrderReadyForLabel(carton, barcode, user);

				PrintJob latestPrintJob = printJobService.findLatestPrintJobForOrder(order.getOrderId());
				if (latestPrintJob == null || latestPrintJob.getStatus() != PrintJobStatus.PRINTED) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
							"The label for order " + order.getOrderId() + " is not printed yet");
				}

				if (scannedLabel == null || !scannedLabel.trim().equals(order.getOrderId())) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Label " + scannedLabel
							+ " does not belong to carton " + barcode + ". Scan the printed label");
				}

				order.setStatus(RetrievalOrderStatus.FINISHED);
				Location gate = findGateForOrder(session, order);

				if (!Constants.CONVEYOR_GATES.contains(gate.getName())) {
					carton.setLocation(gate); // gate manual, ca inainte
					setReturnValue(false);
					return;
				}

				// gate pe banda: cartonul ramane pe OUT, TO-ul activ spune GP-urilor unde sa-l
				// coboare
				new TransportOrderRepository(session)
						.insert(new TransportOrder(carton.getLocation(), gate, TransportOrderStatus.ACTIVE, carton));
				setReturnValue(true);
			}
		}.run();

		// run() a facut deja commit, deci TO-ul exista cand cartonul ajunge la GP1
		if (onConveyor) {
			sendCartonToGateLine(barcode);
		}
	}

	public void reprintLabel(String barcode, String user) {
		new WithSessionAndTransaction<Void>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				LoadUnit carton = findLoadUnitByBarcode(session, barcode);
				RetrievalOrder order = findOrderReadyForLabel(carton, barcode, user);

				PrintJob latestPrintJob = printJobService.findLatestPrintJobForOrder(order.getOrderId());
				if (latestPrintJob == null) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
							"There is no label to reprint for order " + order.getOrderId());
				}
				if (latestPrintJob.getStatus() == PrintJobStatus.NEW) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
							"The label for order " + order.getOrderId() + " is still printing");
				}

				printJobService.resetPrintJobForReprint(latestPrintJob);
			}
		}.run();
	}
}