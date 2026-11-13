package ro.demo.usersbackend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ro.demo.usersbackend.dto.InventoryLoadUnitResponse;
import ro.demo.usersbackend.dto.InventoryOrderLineResponse;
import ro.demo.usersbackend.dto.InventoryOrderLineStartRequest;
import ro.demo.usersbackend.dto.InventoryOrderResponse;
import ro.demo.usersbackend.dto.ScannedInventoryItemRequest;
import ro.demo.usersbackend.entity.Article;
import ro.demo.usersbackend.entity.ArticlePackagingUnitType;
import ro.demo.usersbackend.entity.InventoryOrder;
import ro.demo.usersbackend.entity.InventoryOrderLine;
import ro.demo.usersbackend.entity.InventoryOrderLineItem;
import ro.demo.usersbackend.entity.InventoryOrderLineStatus;
import ro.demo.usersbackend.entity.InventoryOrderStatus;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.LoadUnitType;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.PackagingUnitType;
import ro.demo.usersbackend.entity.Reservation;
import ro.demo.usersbackend.entity.RetrievalOrder;
import ro.demo.usersbackend.entity.RetrievalOrderLine;
import ro.demo.usersbackend.entity.RetrievalOrderLineStatus;
import ro.demo.usersbackend.entity.RetrievalOrderStatus;
import ro.demo.usersbackend.entity.StockAdjustment;
import ro.demo.usersbackend.entity.StockAdjustmentReason;
import ro.demo.usersbackend.entity.StockItem;
import ro.demo.usersbackend.entity.StorageLocation;
import ro.demo.usersbackend.entity.TransitLocation;
import ro.demo.usersbackend.repository.ArticlePackagingUnitTypeRepository;
import ro.demo.usersbackend.repository.ArticleRepository;
import ro.demo.usersbackend.repository.InventoryOrderLineItemRepository;
import ro.demo.usersbackend.repository.InventoryOrderLineRepository;
import ro.demo.usersbackend.repository.InventoryOrderRepository;
import ro.demo.usersbackend.repository.LoadUnitRepository;
import ro.demo.usersbackend.repository.PackagingUnitTypeRepository;
import ro.demo.usersbackend.repository.ReservationRepository;
import ro.demo.usersbackend.repository.StockAdjustmentRepository;
import ro.demo.usersbackend.repository.StockItemRepository;
import ro.demo.usersbackend.repository.StorageLocationRepository;

@Service
@Transactional
public class InventoryOrderService {
	private final InventoryOrderRepository inventoryOrderRepository;
	private final InventoryOrderLineRepository inventoryOrderLineRepository;
	private final InventoryOrderLineItemRepository inventoryOrderLineItemRepository;
	private final StockAdjustmentRepository inventoryDifferenceRepository;
	private final LoadUnitRepository loadUnitRepository;
	private final StockItemRepository stockItemRepository;
	private final ReservationRepository reservationRepository;
	private final ArticleRepository articleRepository;
	private final PackagingUnitTypeRepository packagingUnitTypeRepository;
	private final ArticlePackagingUnitTypeRepository articlePackagingUnitTypeRepository;
	private final StorageLocationRepository storageLocationRepository;
	private static final Logger log = LoggerFactory.getLogger(InventoryOrderService.class);

	public InventoryOrderService(InventoryOrderRepository inventoryOrderRepository,
			InventoryOrderLineRepository inventoryOrderLineRepository,
			InventoryOrderLineItemRepository inventoryOrderLineItemRepository,
			StockAdjustmentRepository inventoryDifferenceRepository, LoadUnitRepository loadUnitRepository,
			StockItemRepository stockItemRepository, ReservationRepository reservationRepository,
			ArticleRepository articleRepository, PackagingUnitTypeRepository packagingUnitTypeRepository,
			ArticlePackagingUnitTypeRepository articlePackagingUnitTypeRepository,
			StorageLocationRepository storageLocationRepository) {
		this.inventoryOrderRepository = inventoryOrderRepository;
		this.inventoryOrderLineRepository = inventoryOrderLineRepository;
		this.inventoryOrderLineItemRepository = inventoryOrderLineItemRepository;
		this.inventoryDifferenceRepository = inventoryDifferenceRepository;
		this.loadUnitRepository = loadUnitRepository;
		this.stockItemRepository = stockItemRepository;
		this.reservationRepository = reservationRepository;
		this.articleRepository = articleRepository;
		this.packagingUnitTypeRepository = packagingUnitTypeRepository;
		this.articlePackagingUnitTypeRepository = articlePackagingUnitTypeRepository;
		this.storageLocationRepository = storageLocationRepository;
	}

	@Transactional(readOnly = true)
	public InventoryLoadUnitResponse getEligibleLoadUnit(String barcode) {
		LoadUnit loadUnit = findEligibleLoadUnit(barcode);
		return new InventoryLoadUnitResponse(loadUnit.getId(), loadUnit.getBarcode(), loadUnit.getType().name(),
				loadUnit.getLocation().getName());
	}

	@Transactional(readOnly = true)
	public List<InventoryOrderResponse> getStartedInventoryOrders() {
		return inventoryOrderRepository.findByStatusOrderByCreatedAtDesc(InventoryOrderStatus.STARTED).stream()
				.map(this::toOrderResponse).toList();
	}

	public InventoryOrderResponse createInventoryOrder() {
		InventoryOrder order = new InventoryOrder();
		order.setCode("IO-" + UUID.randomUUID());
		order.setCreatedAt(LocalDateTime.now());
		order.setReasonMessage("INVENTORY");
		order.setStatus(InventoryOrderStatus.STARTED);
		return toOrderResponse(inventoryOrderRepository.save(order));
	}

	@Transactional(readOnly = true)
	public InventoryOrderResponse getStartedInventoryOrder(String code) {
		InventoryOrder order = inventoryOrderRepository.findByCodeAndStatus(code, InventoryOrderStatus.STARTED)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Started inventory order was not found: " + code));
		return toOrderResponse(order);
	}

	public InventoryOrderLineResponse startInventoryOrderLine(Long orderId, InventoryOrderLineStartRequest request) {
		InventoryOrder order = findStartedOrder(orderId);
		if (inventoryOrderLineRepository.existsByInventoryOrder_IdAndStatus(orderId,
				InventoryOrderLineStatus.STARTED)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Finish the started load-unit line before starting another.");
		}
		InventoryOrderLine line = inventoryOrderLineRepository.findByInventoryOrder_IdOrderByIdAsc(orderId).stream()
				.filter(candidate -> candidate.getStatus() == InventoryOrderLineStatus.NEW)
				.findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
						"This inventory order has no new lines to scan."));
		Location location = line.getLocation();
		if (location == null || location instanceof TransitLocation) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"The current inventory line must have a non-transit location.");
		}
		LoadUnit foundLoadUnit = loadUnitRepository.findByBarcode(request.loadUnitBarcode()).orElseGet(() -> {
			LoadUnit created = new LoadUnit();
			created.setBarcode(request.loadUnitBarcode());
			created.setType(LoadUnitType.PALLET);
			created.setLocation(location);
			return loadUnitRepository.save(created);
		});
		LoadUnit loadUnit = findEligibleLoadUnit(foundLoadUnit.getBarcode());
		if (loadUnit.getLocation().getId() != location.getId()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Load unit " + loadUnit.getBarcode() + " is not at the current line location "
							+ location.getName() + ".");
		}
		if (inventoryOrderLineRepository.findByInventoryOrder_IdOrderByIdAsc(orderId).stream()
				.anyMatch(candidate -> candidate.getLoadUnit() != null
						&& candidate.getLoadUnit().getId().equals(loadUnit.getId()))) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Load unit is already in this inventory order: " + request.loadUnitBarcode());
		}
		line.setLoadUnit(loadUnit);
		line.setLoadUnitBarcode(loadUnit.getBarcode());
		line.setLoadUnitType(loadUnit.getType().name());
		line.setLocationName(loadUnit.getLocation().getName());
		line.setStatus(InventoryOrderLineStatus.STARTED);
		line.setCompletedAt(null);
		if (location instanceof StorageLocation storageLocation) {
			storageLocation.setLoadUnit(loadUnit);
			storageLocationRepository.save(storageLocation);
		}
		return toLineResponse(inventoryOrderLineRepository.save(line));
	}

	public InventoryOrderLineResponse saveInventoryOrderLineItems(Long orderId, Long lineId,
			List<ScannedInventoryItemRequest> items) {
		InventoryOrderLine line = findStartedLine(orderId, lineId);
		inventoryOrderLineItemRepository.deleteByInventoryOrderLine_Id(lineId);
		for (ScannedInventoryItemRequest request : items) {
			InventoryOrderLineItem item = new InventoryOrderLineItem();
			item.setInventoryOrderLine(line);
			item.setArticleBarcode(request.articleBarcode());
			item.setArticleDescription(request.articleDescription());
			item.setPackagingUnitName(request.packagingUnitName());
			item.setQuantity(request.quantity());
			inventoryOrderLineItemRepository.save(item);
		}
		return toLineResponse(line);
	}

	public InventoryOrderLineResponse finishInventoryOrderLine(Long orderId, Long lineId) {
		InventoryOrderLine line = findStartedLine(orderId, lineId);
		line.setStatus(InventoryOrderLineStatus.FINISHED);
		line.setCompletedAt(LocalDateTime.now());
		line = inventoryOrderLineRepository.save(line);
		if (!line.isStockAdjustmentsCalculated()) {
			createInventoryAdjustments(line);
			line.setStockAdjustmentsCalculated(true);
		}
		if (!line.isStockUpdated()) {
			updateInventoryOrderLineStock(line);
			line.setStockUpdated(true);
		}
		return toLineResponse(inventoryOrderLineRepository.save(line));
	}

	public InventoryOrderLineResponse finishEmptyInventoryOrderLine(Long orderId, Long lineId) {
		InventoryOrder order = findStartedOrder(orderId);
		InventoryOrderLine line = inventoryOrderLineRepository.findById(lineId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Inventory order line was not found: " + lineId));
		if (!line.getInventoryOrder().getId().equals(order.getId())) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND,
					"Inventory order line does not belong to this order.");
		}
		if (line.getStatus() != InventoryOrderLineStatus.NEW || line.getLoadUnit() != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Only a new line without a scanned load unit can be finished as empty.");
		}
		List<LoadUnit> loadUnits = loadUnitRepository.findByLocationOrderByIdAsc(line.getLocation());
		if (!loadUnits.isEmpty()) {
			line.setLoadUnitBarcode(loadUnits.get(0).getBarcode());
			line.setLoadUnitType(loadUnits.get(0).getType().name());
			line.setLocationName(line.getLocation().getName());
		}
		Map<ItemKey, Integer> expectedQuantities = getExpectedQuantities(getLocationStockItems(line.getLocation()));
		List<StockAdjustment> adjustments = new ArrayList<>();
		for (Map.Entry<ItemKey, Integer> expected : expectedQuantities.entrySet()) {
			adjustments.add(createDifference(line, expected.getKey(), null, expected.getValue(), 0,
					-expected.getValue()));
		}
		inventoryDifferenceRepository.saveAll(adjustments);
		for (LoadUnit missingLoadUnit : loadUnits) {
			List<StockItem> missingStockItems = stockItemRepository.findByLoadUnitOrderByIdAsc(missingLoadUnit);
			requeueActiveOrders(missingStockItems);
			if (!reservationRepository.findByStockItemIn(missingStockItems).isEmpty()) {
				throw new ResponseStatusException(HttpStatus.CONFLICT,
						"Cannot remove a missing load unit while it still has reservations.");
			}
			stockItemRepository.deleteAll(missingStockItems);
			if (line.getLocation() instanceof StorageLocation storageLocation
					&& storageLocation.getLoadUnit() != null
					&& storageLocation.getLoadUnit().getId().equals(missingLoadUnit.getId())) {
				storageLocation.setLoadUnit(null);
				storageLocationRepository.save(storageLocation);
			}
			loadUnitRepository.delete(missingLoadUnit);
		}
		line.setStatus(InventoryOrderLineStatus.FINISHED);
		line.setCompletedAt(LocalDateTime.now());
		line.setStockAdjustmentsCalculated(true);
		return toLineResponse(inventoryOrderLineRepository.save(line));
	}

	private void createInventoryAdjustments(InventoryOrderLine line) {
		LoadUnit loadUnit = findEligibleLoadUnit(line.getLoadUnitBarcode());
		List<ScannedInventoryItemRequest> requestItems = inventoryOrderLineItemRepository
				.findByInventoryOrderLine_IdOrderByIdAsc(line.getId()).stream()
				.map(item -> new ScannedInventoryItemRequest(item.getArticleBarcode(), item.getArticleDescription(),
						item.getPackagingUnitName(), item.getQuantity()))
				.toList();
		Map<ItemKey, ScannedInventoryItemRequest> scannedItems = getScannedItems(requestItems);
		List<StockItem> stockItems = getLocationStockItems(line.getLocation());
		Map<ItemKey, Integer> expectedQuantities = getExpectedQuantities(stockItems);
		Set<ItemKey> allItems = new LinkedHashSet<>();
		allItems.addAll(expectedQuantities.keySet());
		allItems.addAll(scannedItems.keySet());

		List<StockAdjustment> adjustments = new ArrayList<>();
		for (ItemKey key : allItems) {
			int expected = expectedQuantities.getOrDefault(key, 0);
			ScannedInventoryItemRequest scanned = scannedItems.get(key);
			int counted = scanned == null ? 0 : scanned.quantity();
			int difference = counted - expected;
			if (difference != 0) {
				adjustments.add(createDifference(line, key, scanned, expected, counted, difference));
			}
		}
		inventoryDifferenceRepository.saveAll(adjustments);
	}

	private List<StockItem> getLocationStockItems(Location location) {
		return loadUnitRepository.findByLocationOrderByIdAsc(location).stream()
				.flatMap(loadUnit -> stockItemRepository.findByLoadUnitOrderByIdAsc(loadUnit).stream())
				.toList();
	}

	private void updateInventoryOrderLineStock(InventoryOrderLine line) {
		LoadUnit loadUnit = findEligibleLoadUnit(line.getLoadUnitBarcode());
		List<ScannedInventoryItemRequest> requestItems = inventoryOrderLineItemRepository
				.findByInventoryOrderLine_IdOrderByIdAsc(line.getId()).stream()
				.map(item -> new ScannedInventoryItemRequest(item.getArticleBarcode(), item.getArticleDescription(),
						item.getPackagingUnitName(), item.getQuantity()))
				.toList();
		Map<ItemKey, ScannedInventoryItemRequest> scannedItems = getScannedItems(requestItems);
		List<StockItem> stockItems = stockItemRepository.findByLoadUnitOrderByIdAsc(loadUnit);
		if (!stockItems.isEmpty()) {
			for (Reservation reservation : reservationRepository.findByStockItemIn(stockItems)) {
				reservation.getStockItem().setReservation(reservation);
			}
		}
		updateExistingStockItems(loadUnit, stockItems, scannedItems);
	}

	public void finishInventoryOrder(Long orderId) {
		InventoryOrder order = findStartedOrder(orderId);
		List<InventoryOrderLine> lines = inventoryOrderLineRepository.findByInventoryOrder_IdOrderByIdAsc(orderId);
		if (lines.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"An inventory order must contain at least one completed load-unit line.");
		}
		if (lines.stream().anyMatch(line -> line.getStatus() != InventoryOrderLineStatus.FINISHED)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Finish every load-unit line before finishing the inventory order.");
		}

		LocalDateTime now = LocalDateTime.now();
		for (InventoryOrderLine line : lines) {
			if (line.getLoadUnit() == null) {
				continue;
			}
			if (!line.isStockAdjustmentsCalculated()) {
				createInventoryAdjustments(line);
				line.setStockAdjustmentsCalculated(true);
			}
			if (!line.isStockUpdated()) {
				updateInventoryOrderLineStock(line);
				line.setStockUpdated(true);
			}
			inventoryOrderLineRepository.save(line);

		}

		order.setStatus(InventoryOrderStatus.FINISHED);
		order.setFinishedAt(now);
		inventoryOrderRepository.save(order);
	}

	private InventoryOrder findStartedOrder(Long orderId) {
		InventoryOrder order = inventoryOrderRepository.findById(orderId).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory order was not found: " + orderId));
		if (order.getStatus() != InventoryOrderStatus.STARTED) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Inventory order is already finished.");
		}
		return order;
	}

	private InventoryOrderLine findStartedLine(Long orderId, Long lineId) {
		findStartedOrder(orderId);
		InventoryOrderLine line = inventoryOrderLineRepository.findById(lineId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Inventory order line was not found: " + lineId));
		if (!line.getInventoryOrder().getId().equals(orderId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND,
					"Inventory order line does not belong to this order.");
		}
		if (line.getStatus() != InventoryOrderLineStatus.STARTED) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Inventory order line is already finished.");
		}
		return line;
	}

	private InventoryOrderResponse toOrderResponse(InventoryOrder order) {
		List<InventoryOrderLineResponse> lines = inventoryOrderLineRepository
				.findByInventoryOrder_IdOrderByIdAsc(order.getId()).stream().map(this::toLineResponse).toList();
		return new InventoryOrderResponse(order.getId(), order.getCode(), order.getCreatedAt(), order.getFinishedAt(),
				order.getStatus().name(), lines);
	}

	private InventoryOrderLineResponse toLineResponse(InventoryOrderLine line) {
		List<ScannedInventoryItemRequest> items = inventoryOrderLineItemRepository
				.findByInventoryOrderLine_IdOrderByIdAsc(line.getId()).stream()
				.map(item -> new ScannedInventoryItemRequest(item.getArticleBarcode(), item.getArticleDescription(),
						item.getPackagingUnitName(), item.getQuantity()))
				.toList();
		Long loadUnitId = line.getLoadUnit() == null ? null : line.getLoadUnit().getId();
		Long locationId = line.getLocation() == null ? null : (long) line.getLocation().getId();
		String locationName = line.getLocationName() != null ? line.getLocationName()
				: line.getLocation() == null ? null : line.getLocation().getName();
		return new InventoryOrderLineResponse(line.getId(), locationId, loadUnitId, line.getLoadUnitBarcode(),
				line.getLoadUnitType(), locationName, line.getStatus().name(), items);
	}

	private LoadUnit findEligibleLoadUnit(String barcode) {
		LoadUnit loadUnit = loadUnitRepository.findByBarcode(barcode).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Load unit was not found: " + barcode));
		if (loadUnit.getLocation() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Load unit has no location: " + barcode);
		}
		if (loadUnit.getLocation() instanceof TransitLocation) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Load units in transit cannot be added to an inventory order: " + barcode);
		}
		return loadUnit;
	}

	private Map<ItemKey, Integer> getExpectedQuantities(List<StockItem> stockItems) {
		Map<ItemKey, Integer> quantities = new LinkedHashMap<>();

		for (StockItem stockItem : stockItems) {
			ItemKey key = getItemKey(stockItem);
			quantities.merge(key, stockItem.getQuantity(), Integer::sum);
		}

		return quantities;
	}

	private void updateExistingStockItems(LoadUnit loadUnit, List<StockItem> stockItems,
			Map<ItemKey, ScannedInventoryItemRequest> scannedItems) {
		if (loadUnit == null) {
			if (!scannedItems.isEmpty()) {
				log.warn("Cannot update StockItems because load unit was not found.");
			}
			return;
		}

		Map<ItemKey, List<StockItem>> itemsByKey = new LinkedHashMap<>();
		for (StockItem stockItem : stockItems) {
			itemsByKey.computeIfAbsent(getItemKey(stockItem), key -> new ArrayList<>()).add(stockItem);
		}

		Set<ItemKey> allItems = new LinkedHashSet<>(itemsByKey.keySet());
		allItems.addAll(scannedItems.keySet());

		for (ItemKey key : allItems) {
			List<StockItem> existingItems = itemsByKey.getOrDefault(key, List.of());
			ScannedInventoryItemRequest scanned = scannedItems.get(key);
			int countedQuantity = scanned == null ? 0 : scanned.quantity();

			int reservedQuantity = getReservedQuantity(existingItems);
			if (countedQuantity < reservedQuantity) {
				requeueActiveOrders(existingItems);
				reservedQuantity = getReservedQuantity(existingItems);
			}

			if (countedQuantity < reservedQuantity) {
				reconcileShortage(existingItems, countedQuantity, key);
				continue;
			}

			List<StockItem> unreservedItems = existingItems.stream()
					.filter(stockItem -> stockItem.getReservation() == null).toList();
			if (!unreservedItems.isEmpty()) {
				stockItemRepository.deleteAll(unreservedItems);
			}

			int unreservedCountedQuantity = countedQuantity - reservedQuantity;
			if (unreservedCountedQuantity <= 0) {
				continue;
			}

			Optional<ArticlePackagingUnitType> articlePackagingUnitType = existingItems.stream()
					.map(StockItem::getArticlePackagingUnitType).findFirst();
			if (articlePackagingUnitType.isEmpty()) {
				articlePackagingUnitType = findArticlePackagingUnitType(key);
			}

			if (articlePackagingUnitType.isPresent()) {
				stockItemRepository
						.save(new StockItem(articlePackagingUnitType.get(), unreservedCountedQuantity, loadUnit));
			} else {
				log.warn("Skipping stock update for unknown article/package: {} / {}", key.articleBarcode(),
						key.packagingUnitName());
			}
		}
	}

	private int getReservedQuantity(List<StockItem> stockItems) {
		return stockItems.stream().filter(stockItem -> stockItem.getReservation() != null)
				.mapToInt(StockItem::getQuantity).sum();
	}

	private void requeueActiveOrders(List<StockItem> stockItems) {
		Set<RetrievalOrder> ordersToRequeue = new LinkedHashSet<>();
		for (StockItem stockItem : stockItems) {
			Reservation reservation = stockItem.getReservation();
			if (reservation == null) {
				continue;
			}

			RetrievalOrder order = reservation.getRetrievalOrderLine().getOrder();
			if (order.getStatus() == RetrievalOrderStatus.ACTIVE) {
				ordersToRequeue.add(order);
			}
		}

		for (RetrievalOrder order : ordersToRequeue) {
			List<Reservation> reservations = new ArrayList<>();
			for (RetrievalOrderLine line : order.getLines()) {
				for (Reservation reservation : line.getReservations()) {
					reservation.getStockItem().setReservation(null);
					reservations.add(reservation);
				}
				line.getReservations().clear();
				line.setStatus(RetrievalOrderLineStatus.NEW);
			}

			stockItemRepository.flush();
			reservationRepository.deleteAll(reservations);
			order.setStatus(RetrievalOrderStatus.NEW);
			log.warn("Released reservations for order {} after an inventory shortage; queued it for re-reservation.",
					order.getOrderId());
		}
	}

	private void reconcileShortage(List<StockItem> stockItems, int countedQuantity, ItemKey key) {
		int remaining = countedQuantity;
		for (StockItem stockItem : stockItems) {
			if (stockItem.getReservation() == null) {
				stockItemRepository.delete(stockItem);
				continue;
			}

			int previousQuantity = stockItem.getQuantity();
			int correctedQuantity = Math.min(previousQuantity, remaining);
			stockItem.setQuantity(correctedQuantity);
			remaining -= correctedQuantity;

			RetrievalOrder order = stockItem.getReservation().getRetrievalOrderLine().getOrder();
			log.warn(
					"Inventory shortage for reserved order {} (status {}): article/package {} / {} corrected from {} to {}.",
					order.getOrderId(), order.getStatus(), key.articleBarcode(), key.packagingUnitName(),
					previousQuantity, correctedQuantity);
		}
	}

	private ItemKey getItemKey(StockItem stockItem) {
		ArticlePackagingUnitType articlePackaging = stockItem.getArticlePackagingUnitType();

		return new ItemKey(articlePackaging.getArticle().getBarcode(),
				articlePackaging.getPackagingUnitType().getName());
	}

	private Map<ItemKey, ScannedInventoryItemRequest> getScannedItems(List<ScannedInventoryItemRequest> items) {
		Map<ItemKey, ScannedInventoryItemRequest> scannedItems = new LinkedHashMap<>();

		for (ScannedInventoryItemRequest item : items) {
			ItemKey key = new ItemKey(item.articleBarcode(), item.packagingUnitName());
			ScannedInventoryItemRequest previous = scannedItems.get(key);

			if (previous == null) {
				scannedItems.put(key, item);
			} else {
				String description = previous.articleDescription() != null ? previous.articleDescription()
						: item.articleDescription();

				scannedItems.put(key, new ScannedInventoryItemRequest(item.articleBarcode(), description,
						item.packagingUnitName(), previous.quantity() + item.quantity()));
			}
		}

		return scannedItems;
	}

	private StockAdjustment createDifference(InventoryOrderLine line, ItemKey key, ScannedInventoryItemRequest scanned,
			int expected, int counted, int difference) {
		Article article = articleRepository.findByBarcode(key.articleBarcode()).orElse(null);
		PackagingUnitType packagingUnitType = packagingUnitTypeRepository.findByName(key.packagingUnitName())
				.orElse(null);
		ArticlePackagingUnitType articlePackagingUnitType = findArticlePackagingUnitType(article, packagingUnitType);

		StockAdjustment inventoryDifference = new StockAdjustment();
		inventoryDifference.setInventoryOrderLine(line);
		inventoryDifference.setArticle(article);
		inventoryDifference.setArticleBarcode(key.articleBarcode());
		inventoryDifference.setArticleDescription(
				scanned != null && scanned.articleDescription() != null ? scanned.articleDescription()
						: article != null ? article.getDescription() : null);
		inventoryDifference.setPackagingUnitType(packagingUnitType);
		inventoryDifference.setArticlePackagingUnitType(articlePackagingUnitType);
		inventoryDifference.setExpectedQuantity(expected);
		inventoryDifference.setCountedQuantity(counted);
		inventoryDifference.setDifferenceQuantity(difference);
		inventoryDifference.setReasonMessage(StockAdjustmentReason.INVENTORY);

		return inventoryDifference;
	}

	private ArticlePackagingUnitType findArticlePackagingUnitType(Article article,
			PackagingUnitType packagingUnitType) {
		if (article == null || packagingUnitType == null) {
			return null;
		}

		Optional<ArticlePackagingUnitType> result = articlePackagingUnitTypeRepository
				.findByArticleAndPackagingUnitType(article, packagingUnitType);

		return result.orElse(null);
	}

	private Optional<ArticlePackagingUnitType> findArticlePackagingUnitType(ItemKey key) {
		return articleRepository.findByBarcode(key.articleBarcode())
				.flatMap(article -> packagingUnitTypeRepository.findByName(key.packagingUnitName())
						.flatMap(packagingUnitType -> articlePackagingUnitTypeRepository
								.findByArticleAndPackagingUnitType(article, packagingUnitType)));
	}

	private record ItemKey(String articleBarcode, String packagingUnitName) {
	}
}
