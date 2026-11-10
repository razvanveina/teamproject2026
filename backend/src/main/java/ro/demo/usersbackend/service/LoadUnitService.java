package ro.demo.usersbackend.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import ro.demo.usersbackend.entity.Destination;
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

	public LoadUnitService(EntityManager entityManager, StockItemRepository stockItemRepository,
			LoadUnitRepository loadUnitRepository, LocationRepository locationRepository,
			DestinationToGateRepository destinationToGateRepository,
			ArticlePackagingUnitTypeRepository articlePackagingUnitTypeRepository) {
		super();
		this.entityManager = entityManager;
		this.stockItemRepository = stockItemRepository;
		this.loadUnitRepository = loadUnitRepository;
		this.locationRepository = locationRepository;
		this.destinationToGateRepository = destinationToGateRepository;
		this.articlePackagingUnitTypeRepository = articlePackagingUnitTypeRepository;
	}

	public LoadUnitResponse findByBarcode(String barcode) {
		LoadUnit loadUnit = loadUnitRepository.findByBarcode(barcode)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Cannot find load unit with the barcode: " + barcode));

		if (loadUnit.getStockItems().get(0).getReservation().getRetrievalOrderLine().getOrder()
				.getStatus() == RetrievalOrderStatus.FINISHED) {
			new ResponseStatusException(HttpStatus.BAD_REQUEST, "The order for this load unit is already finished");
		}

		return new LoadUnitResponse(loadUnit.getBarcode(), loadUnit.getType().toString(),
				loadUnit.getLocation().getName(), createItems(loadUnit.getStockItems()));
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

	@Transactional
	public void finishLoadUnit(String barcode) {
		LoadUnit loadUnit = loadUnitRepository.findByBarcode(barcode)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Cannot find load unit with the barcode: " + barcode));

		RetrievalOrder order = loadUnit.getStockItems().get(0).getReservation().getRetrievalOrderLine().getOrder();

		if (order.getStatus() != RetrievalOrderStatus.PICKED) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order status must be PICKED");
		} else {
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
			} else {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"All lines status must be PICKED. There are " + lineCount + " lines with a different status");
			}
		}

		Destination dest = order.getDestination();

		DestinationToGate destinationToGate = destinationToGateRepository.findByDestinationName(dest.getName())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Could not find a DestinationToGate object from the name: " + dest.getName()));

		loadUnit.setLocation(destinationToGate.getGate());
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