package ro.demo.usersbackend.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityManager;
import ro.demo.usersbackend.dto.DecantingItem;
import ro.demo.usersbackend.dto.DecantingResponse;
import ro.demo.usersbackend.dto.DecantingTransferRequest;
import ro.demo.usersbackend.entity.ArticlePackagingUnitType;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.LoadUnitType;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.StockItem;
import ro.demo.usersbackend.entity.TransitLocation;
import ro.demo.usersbackend.repository.LoadUnitRepository;
import ro.demo.usersbackend.repository.LocationRepository;
import ro.demo.usersbackend.repository.StockItemRepository;

@Service
public class DecantingService {
	private static final String DECANTING_LOCATION = "DECANTING";

	private final LoadUnitRepository loadUnitRepository;
	private final StockItemRepository stockItemRepository;
	private final LocationRepository locationRepository;
	private final EntityManager entityManager;

	public DecantingService(LoadUnitRepository loadUnitRepository, StockItemRepository stockItemRepository,
			LocationRepository locationRepository, EntityManager entityManager) {
		this.loadUnitRepository = loadUnitRepository;
		this.stockItemRepository = stockItemRepository;
		this.locationRepository = locationRepository;
		this.entityManager = entityManager;
	}

	// userul scanează LU-ul → se creează primul tote
	@Transactional
	public DecantingResponse start(String loadUnitBarcode) {
		LoadUnit loadUnit = findLoadUnit(loadUnitBarcode);
		if (loadUnit.getType() == LoadUnitType.TOTE) {
			throw badRequest("Load unit " + loadUnitBarcode + " is a tote. Scan a pallet or a carton.");
		}

		List<StockItem> items = availableItems(loadUnit);
		if (items.isEmpty()) {
			throw badRequest("Load unit " + loadUnitBarcode + " is empty.");
		}

		return response(loadUnit, createTote(), items);
	}

	@Transactional
	public DecantingResponse transfer(DecantingTransferRequest request) {
		LoadUnit loadUnit = findLoadUnit(request.loadUnitBarcode());
		LoadUnit tote = findLoadUnit(request.toteBarcode());
		if (tote.getType() != LoadUnitType.TOTE) {
			throw badRequest(request.toteBarcode() + " is not a tote.");
		}

		StockItem item = availableItems(loadUnit).stream()
				.filter(s -> s.getArticlePackagingUnitType().getBarcode().equals(request.packagingBarcode()))
				.findFirst()
				.orElseThrow(() -> badRequest("Item " + request.packagingBarcode() + " is not on load unit "
						+ request.loadUnitBarcode() + "."));

		int quantity = request.quantity();
		if (quantity <= 0) {
			throw badRequest("Quantity must be greater than 0.");
		}
		if (quantity > item.getQuantity()) {
			throw badRequest("Cannot move " + quantity + ": only " + item.getQuantity() + " available.");
		}

		if (quantity == item.getQuantity()) {
			item.setLoadUnit(tote); 
		} else {
			item.setQuantity(item.getQuantity() - quantity);
			stockItemRepository.save(new StockItem(item.getArticlePackagingUnitType(), quantity, tote));
		}

		List<StockItem> remaining = availableItems(loadUnit);
		if (remaining.isEmpty()) {
			return new DecantingResponse(loadUnit.getBarcode(), null, List.of(), true);
		}
		return response(loadUnit, createTote(), remaining);
	}

	private List<StockItem> availableItems(LoadUnit loadUnit) {
		return stockItemRepository.findByLoadUnitAndReservationIsNullOrderByIdAsc(loadUnit);
	}

	private LoadUnit createTote() {
		Location decanting = locationRepository.findByName(DECANTING_LOCATION)
				.orElseGet(() -> locationRepository.save(new TransitLocation(DECANTING_LOCATION)));

		LoadUnit tote = new LoadUnit();
		tote.setBarcode(generateToteBarcode());
		tote.setType(LoadUnitType.TOTE);
		tote.setLocation(decanting);
		return loadUnitRepository.save(tote);
	}

	private String generateToteBarcode() {
		Number result = (Number) entityManager.createNativeQuery("SELECT TOTE_SEQUENCE.NEXTVAL FROM DUAL")
				.getSingleResult();
		return String.format("%06d", result.longValue());
	}

	private LoadUnit findLoadUnit(String barcode) {
		return loadUnitRepository.findByBarcode(barcode)
				.orElseThrow(() -> badRequest("Load unit " + barcode + " not found."));
	}

	private DecantingResponse response(LoadUnit loadUnit, LoadUnit tote, List<StockItem> items) {
		return new DecantingResponse(loadUnit.getBarcode(), tote.getBarcode(),
				items.stream().map(this::toItem).toList(), false);
	}

	private DecantingItem toItem(StockItem stockItem) {
		ArticlePackagingUnitType packaging = stockItem.getArticlePackagingUnitType();
		return new DecantingItem(packaging.getBarcode(), packaging.getArticle().getBarcode(),
				packaging.getArticle().getDescription(), packaging.getPackagingUnitType().getName(),
				stockItem.getQuantity());
	}

	private static ResponseStatusException badRequest(String message) {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
	}
}