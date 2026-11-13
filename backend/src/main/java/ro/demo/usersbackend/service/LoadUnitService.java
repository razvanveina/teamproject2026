package ro.demo.usersbackend.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import ro.demo.usersbackend.entity.RetrievalOrder;
import ro.demo.usersbackend.entity.RetrievalOrderLine;
import ro.demo.usersbackend.entity.RetrievalOrderLineStatus;
import ro.demo.usersbackend.entity.RetrievalOrderStatus;
import ro.demo.usersbackend.entity.StockItem;
import ro.demo.usersbackend.repository.ArticlePackagingUnitTypeRepository;
import ro.demo.usersbackend.repository.DestinationToGateRepository;
import ro.demo.usersbackend.repository.LoadUnitRepository;
import ro.demo.usersbackend.repository.LocationRepository;
import ro.demo.usersbackend.repository.StockItemRepository;

@Service
@Transactional
public class LoadUnitService {
	@PersistenceContext
	private EntityManager entityManager;

	private final StockItemRepository stockItemRepository;
	private final LoadUnitRepository loadUnitRepository;
	private final LocationRepository locationRepository;
	private final DestinationToGateRepository destinationToGateRepository;
	private final ArticlePackagingUnitTypeRepository articlePackagingUnitTypeRepository;
	private final TransportOrderService transportOrderService;

	private RetrievalOrder orderOf(LoadUnit loadUnit) {
		return loadUnit.getStockItems().stream().filter(s -> s.getReservation() != null)
				.map(s -> s.getReservation().getRetrievalOrderLine().getOrder()).findFirst()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Load unit " + loadUnit.getBarcode() + " does not belong to any order"));
	}

	private LoadUnit findLoadUnit(String barcode) {
		return loadUnitRepository.findByBarcode(barcode)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Cannot find load unit with the barcode: " + barcode));
	}

	public LoadUnitService(EntityManager entityManager, StockItemRepository stockItemRepository,
			LoadUnitRepository loadUnitRepository, LocationRepository locationRepository,
			DestinationToGateRepository destinationToGateRepository,
			ArticlePackagingUnitTypeRepository articlePackagingUnitTypeRepository,
			TransportOrderService transportOrderService) {
		super();
		this.entityManager = entityManager;
		this.stockItemRepository = stockItemRepository;
		this.loadUnitRepository = loadUnitRepository;
		this.locationRepository = locationRepository;
		this.destinationToGateRepository = destinationToGateRepository;
		this.articlePackagingUnitTypeRepository = articlePackagingUnitTypeRepository;
		this.transportOrderService = transportOrderService;
	}

	private void checkLocation(LoadUnit loadUnit, String targetLocation) {
		String location = loadUnit.getLocation() == null ? "nowhere" : loadUnit.getLocation().getName();
		if (!targetLocation.equals(location)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Load unit " + loadUnit.getBarcode() + " is at "
					+ location + ". Bring it to " + targetLocation + " first");
		}
	}

	private Location location(String name) {
		return locationRepository.findByName(name)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
						"Location " + name + " is missing"));
	}

	private void checkShippable(LoadUnit loadUnit) {
		if (loadUnit.getType() != LoadUnitType.CARTON && loadUnit.getType() != LoadUnitType.TOTE) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Load unit " + loadUnit.getBarcode() + " is a " + loadUnit.getType() + ". Scan a carton or a tote");
		}
	}

	private void checkOwner(RetrievalOrder order, String user, String barcode) {
		if (!user.equals(order.getShippingUser())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					order.getShippingUser() == null ? "Scan the load unit " + barcode + " first"
							: "Load unit " + barcode + " is being shipped by " + order.getShippingUser());
		}
	}

	private LoadUnit cartonFor(String cartonBarcode) {
		String barcode = cartonBarcode.trim();
		Optional<LoadUnit> existing = loadUnitRepository.findByBarcode(barcode);

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
			carton.setLocation(location("OUT"));
			return carton;
		}

		LoadUnit carton = new LoadUnit();
		carton.setBarcode(barcode);
		carton.setType(LoadUnitType.CARTON);
		carton.setStockItems(new ArrayList<>());
		carton.setLocation(location("OUT"));
		return loadUnitRepository.save(carton);
	}

	public LoadUnit createLoadUnit(List<ScannedPackage> scannedPackages) {

		Location in = locationRepository.findByName("IN")
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot find a IN location"));

		// nr de package uri per tipul de package
		// se aduna daca e acelasi tip de package scanat iar
		Map<ArticlePackagingUnitType, Integer> packagesPerType = new LinkedHashMap<>();
		for (ScannedPackage scanned : scannedPackages) {
			ArticlePackagingUnitType pack = articlePackagingUnitTypeRepository.findByBarcode(scanned.packageBarcode())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
							"Could not find package with barcode " + scanned.packageBarcode()));

			int total = packagesPerType.getOrDefault(pack, 0);
			packagesPerType.put(pack, total + scanned.packages());
		}

		LoadUnit loadUnit = new LoadUnit();
		loadUnit.setBarcode(generateLoadUnitBarcode());
		loadUnit.setType(LoadUnitType.PALLET);
		loadUnit.setLocation(in);
		loadUnitRepository.save(loadUnit);

		// un stock item per package type
		// cantitatea e nr de package uri acum
		for (Map.Entry<ArticlePackagingUnitType, Integer> entry : packagesPerType.entrySet()) {
			stockItemRepository.save(new StockItem(entry.getKey(), entry.getValue(), loadUnit));
		}
		return loadUnit;
	}

	public LoadUnitResponse findByBarcode(String barcode, String user) {
		LoadUnit loadUnit = findLoadUnit(barcode);
		checkShippable(loadUnit);
		checkLocation(loadUnit, "OUT");
		RetrievalOrder order = orderOf(loadUnit);

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

		List<RetrievalOrder> mine = entityManager
				.createQuery("from RetrievalOrder o where o.shippingUser = :user and o.status = :status",
						RetrievalOrder.class)
				.setParameter("user", user).setParameter("status", RetrievalOrderStatus.PICKED).getResultList();
		for (RetrievalOrder other : mine) {
			if (!other.getId().equals(order.getId())) {
				other.setShippingUser(null);
			}
		}

		order.setShippingUser(user);
		return new LoadUnitResponse(loadUnit.getBarcode(), loadUnit.getType().toString(),
				loadUnit.getLocation().getName(), createItems(loadUnit.getStockItems()));
	}

	public LoadUnitResponse chooseCarton(String toteBarcode, String cartonBarcode, String user) {
		LoadUnit tote = findLoadUnit(toteBarcode);
		if (tote.getType() != LoadUnitType.TOTE) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only a tote needs a carton");
		}
		checkLocation(tote, "OUT");
		RetrievalOrder order = orderOf(tote);
		checkOwner(order, user, toteBarcode);

		LoadUnit carton = cartonFor(cartonBarcode);
		return new LoadUnitResponse(carton.getBarcode(), carton.getType().toString(), carton.getLocation().getName(),
				createItems(carton.getStockItems()));
	}

	public boolean finishLoadUnit(String barcode, String user, String cartonBarcode) {
		LoadUnit loadUnit = findLoadUnit(barcode);
		checkShippable(loadUnit);
		checkLocation(loadUnit, "OUT");
		RetrievalOrder order = orderOf(loadUnit);

		checkOwner(order, user, barcode);

		if (order.getStatus() != RetrievalOrderStatus.PICKED) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order status must be PICKED");
		}

		DestinationToGate destinationToGate = destinationToGateRepository
				.findByDestinationName(order.getDestination().getName())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Could not find a DestinationToGate object from the name: "
								+ order.getDestination().getName()));

		LoadUnit carton = loadUnit;
		if (loadUnit.getType() == LoadUnitType.TOTE) {
			if (cartonBarcode == null || cartonBarcode.isBlank()) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Scan a carton for the items of tote " + barcode + " first");
			}
			carton = cartonFor(cartonBarcode);
			for (StockItem item : new ArrayList<>(loadUnit.getStockItems())) {
				item.setLoadUnit(carton);
			}
		}

		int lineCount = 0;
		for (RetrievalOrderLine line : order.getLines()) {
			if (line.getStatus() == RetrievalOrderLineStatus.PICKED) {
				line.setStatus(RetrievalOrderLineStatus.FINISHED);
			} else {
				lineCount++;
			}
		}

		if (lineCount == 0) {
			order.setStatus(RetrievalOrderStatus.FINISHED);
			carton.setLocation(destinationToGate.getGate());
			if (loadUnit.getType() == LoadUnitType.TOTE) {
				if (order.isAutomatic()) {
					return true; // tote de pe banda, deci controllerul il elibereaza din OUT dupa commit
				}
				transportOrderService.createTransportOrder(loadUnit, location("EMPTY_TOTES")); // flux manual
			}
			return false;
		} else {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"All lines status must be PICKED. There are " + lineCount + " lines with a different status");
		}
	}

	public void releaseLoadUnit(String barcode, String user) {
		LoadUnit loadUnit = findLoadUnit(barcode);
		RetrievalOrder order = orderOf(loadUnit);
		if (user.equals(order.getShippingUser()) && order.getStatus() == RetrievalOrderStatus.PICKED) {
			order.setShippingUser(null);
		}
	}

	private String generateLoadUnitBarcode() {
		Number result = (Number) entityManager.createNativeQuery("SELECT LOADUNIT_SEQUENCE.NEXTVAL FROM DUAL")
				.getSingleResult();

		return String.format("%05d", result.longValue());
	}

	private List<StockItemResponse> createItems(List<StockItem> items) {
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
}