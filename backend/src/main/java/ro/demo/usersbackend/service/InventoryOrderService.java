package ro.demo.usersbackend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
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
import ro.demo.usersbackend.repository.LocationRepository;
import ro.demo.usersbackend.repository.PackagingUnitTypeRepository;
import ro.demo.usersbackend.repository.ReservationRepository;
import ro.demo.usersbackend.repository.StockAdjustmentRepository;
import ro.demo.usersbackend.repository.StockItemRepository;

@Service
public class InventoryOrderService {
	private static final Logger log = LoggerFactory.getLogger(InventoryOrderService.class);

	private InventoryOrder findStartedOrder(Session session, Long orderId) {
		InventoryOrder order = new InventoryOrderRepository(session).findById(orderId).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory order was not found: " + orderId));
		if (order.getStatus() != InventoryOrderStatus.STARTED) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Inventory order is already finished.");
		}
		return order;
	}

	private LoadUnit findEligibleLoadUnit(Session session, String barcode) {
		LoadUnit loadUnit = new LoadUnitRepository(session).findByBarcode(barcode).orElseThrow(
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

	// item-urile scanate pe linie, in forma request-ului
	private List<ScannedInventoryItemRequest> scannedItemsOf(Session session, InventoryOrderLine line) {
		return new InventoryOrderLineItemRepository(session).findByInventoryOrderLineId(line.getId()).stream()
				.map(item -> new ScannedInventoryItemRequest(item.getArticleBarcode(), item.getArticleDescription(),
						item.getPackagingUnitName(), item.getQuantity()))
				.toList();
	}

	private List<StockItem> getLocationStockItems(Session session, Location location) {
		StockItemRepository stockItemRepository = new StockItemRepository(session);
		return new LoadUnitRepository(session).findByLocationOrderByIdAsc(location).stream()
				.flatMap(loadUnit -> stockItemRepository.findByLoadUnitOrderByIdAsc(loadUnit).stream()).toList();
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

	private int getReservedQuantity(List<StockItem> stockItems) {
		return stockItems.stream().filter(stockItem -> stockItem.getReservation() != null)
				.mapToInt(StockItem::getQuantity).sum();
	}

	private void requeueActiveOrders(Session session, List<StockItem> stockItems) {
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

		ReservationRepository reservationRepository = new ReservationRepository(session);
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

			session.flush();
			reservations.forEach(reservationRepository::delete);
			order.setStatus(RetrievalOrderStatus.NEW);
			log.warn("Released reservations for order {} after an inventory shortage; queued it for re-reservation.",
					order.getOrderId());
		}
	}

	private void reconcileShortage(Session session, List<StockItem> stockItems, int countedQuantity, ItemKey key) {
		StockItemRepository stockItemRepository = new StockItemRepository(session);
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

	private LoadUnit createPalletAt(Session session, String barcode, Location location) {
		LoadUnitRepository loadUnitRepository = new LoadUnitRepository(session);
		Location in = new LocationRepository(session).findByName(Constants.IN).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot find location " + Constants.IN));
		for (LoadUnit old : loadUnitRepository.findByLocationOrderByIdAsc(location)) {
			old.setLocation(in);
		}
		session.flush();

		LoadUnit created = new LoadUnit();
		created.setBarcode(barcode);
		created.setType(LoadUnitType.PALLET);
		created.setLocation(location);
		loadUnitRepository.insert(created);
		return created;
	}

	private ArticlePackagingUnitType findArticlePackagingUnitType(Session session, Article article,
			PackagingUnitType packagingUnitType) {
		if (article == null || packagingUnitType == null) {
			return null;
		}

		return new ArticlePackagingUnitTypeRepository(session)
				.findByArticleAndPackagingUnitType(article, packagingUnitType).orElse(null);
	}

	private Optional<ArticlePackagingUnitType> findArticlePackagingUnitType(Session session, ItemKey key) {
		ArticlePackagingUnitTypeRepository articlePackagingUnitTypeRepository = new ArticlePackagingUnitTypeRepository(
				session);
		PackagingUnitTypeRepository packagingUnitTypeRepository = new PackagingUnitTypeRepository(session);
		return new ArticleRepository(session).findByBarcode(key.articleBarcode())
				.flatMap(article -> packagingUnitTypeRepository.findByName(key.packagingUnitName())
						.flatMap(packagingUnitType -> articlePackagingUnitTypeRepository
								.findByArticleAndPackagingUnitType(article, packagingUnitType)));
	}

	private InventoryOrderLine findStartedLine(Session session, Long orderId, Long lineId) {
		findStartedOrder(session, orderId);
		InventoryOrderLine line = new InventoryOrderLineRepository(session).findById(lineId)
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

	private InventoryOrderLineResponse toLineResponse(Session session, InventoryOrderLine line) {
		List<ScannedInventoryItemRequest> items = scannedItemsOf(session, line);
		Long loadUnitId = line.getLoadUnit() == null ? null : line.getLoadUnit().getId();
		Long locationId = line.getLocation() == null ? null : (long) line.getLocation().getId();
		String locationName = line.getLocationName() != null ? line.getLocationName()
				: line.getLocation() == null ? null : line.getLocation().getName();
		return new InventoryOrderLineResponse(line.getId(), locationId, loadUnitId, line.getLoadUnitBarcode(),
				line.getLoadUnitType(), locationName, line.getStatus().name(), items);
	}

	private Map<ItemKey, Integer> getExpectedQuantities(List<StockItem> stockItems) {
		Map<ItemKey, Integer> quantities = new LinkedHashMap<>();

		for (StockItem stockItem : stockItems) {
			ItemKey key = getItemKey(stockItem);
			quantities.merge(key, stockItem.getQuantity(), Integer::sum);
		}

		return quantities;
	}

	private StockAdjustment createDifference(Session session, InventoryOrderLine line, ItemKey key,
			ScannedInventoryItemRequest scanned, int expected, int counted, int difference) {
		Article article = new ArticleRepository(session).findByBarcode(key.articleBarcode()).orElse(null);
		PackagingUnitType packagingUnitType = new PackagingUnitTypeRepository(session)
				.findByName(key.packagingUnitName()).orElse(null);
		ArticlePackagingUnitType articlePackagingUnitType = findArticlePackagingUnitType(session, article,
				packagingUnitType);

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

	private void updateExistingStockItems(Session session, LoadUnit loadUnit, List<StockItem> stockItems,
			Map<ItemKey, ScannedInventoryItemRequest> scannedItems) {
		if (loadUnit == null) {
			if (!scannedItems.isEmpty()) {
				log.warn("Cannot update StockItems because load unit was not found.");
			}
			return;
		}

		StockItemRepository stockItemRepository = new StockItemRepository(session);
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
				requeueActiveOrders(session, existingItems);
				reservedQuantity = getReservedQuantity(existingItems);
			}

			if (countedQuantity < reservedQuantity) {
				reconcileShortage(session, existingItems, countedQuantity, key);
				continue;
			}

			existingItems.stream().filter(stockItem -> stockItem.getReservation() == null)
					.forEach(stockItemRepository::delete);

			int unreservedCountedQuantity = countedQuantity - reservedQuantity;
			if (unreservedCountedQuantity <= 0) {
				continue;
			}

			Optional<ArticlePackagingUnitType> articlePackagingUnitType = existingItems.stream()
					.map(StockItem::getArticlePackagingUnitType).findFirst();
			if (articlePackagingUnitType.isEmpty()) {
				articlePackagingUnitType = findArticlePackagingUnitType(session, key);
			}

			if (articlePackagingUnitType.isPresent()) {
				stockItemRepository
						.insert(new StockItem(articlePackagingUnitType.get(), unreservedCountedQuantity, loadUnit));
			} else {
				log.warn("Skipping stock update for unknown article/package: {} / {}", key.articleBarcode(),
						key.packagingUnitName());
			}
		}
	}

	private InventoryOrderResponse toOrderResponse(Session session, InventoryOrder order) {
		List<InventoryOrderLineResponse> lines = new InventoryOrderLineRepository(session)
				.findByInventoryOrderId(order.getId()).stream().map(line -> toLineResponse(session, line)).toList();
		return new InventoryOrderResponse(order.getId(), order.getCode(), order.getCreatedAt(), order.getFinishedAt(),
				order.getStatus().name(), lines);
	}

	private void createInventoryAdjustments(Session session, InventoryOrderLine line) {
		Map<ItemKey, ScannedInventoryItemRequest> scannedItems = getScannedItems(scannedItemsOf(session, line));
		List<StockItem> stockItems = getLocationStockItems(session, line.getLocation());
		Map<ItemKey, Integer> expectedQuantities = getExpectedQuantities(stockItems);
		Set<ItemKey> allItems = new LinkedHashSet<>();
		allItems.addAll(expectedQuantities.keySet());
		allItems.addAll(scannedItems.keySet());

		StockAdjustmentRepository adjustmentRepository = new StockAdjustmentRepository(session);
		for (ItemKey key : allItems) {
			int expected = expectedQuantities.getOrDefault(key, 0);
			ScannedInventoryItemRequest scanned = scannedItems.get(key);
			int counted = scanned == null ? 0 : scanned.quantity();
			int difference = counted - expected;
			if (difference != 0) {
				adjustmentRepository
						.insert(createDifference(session, line, key, scanned, expected, counted, difference));
			}
		}
	}

	private void updateInventoryOrderLineStock(Session session, InventoryOrderLine line) {
		LoadUnit loadUnit = findEligibleLoadUnit(session, line.getLoadUnitBarcode());
		Map<ItemKey, ScannedInventoryItemRequest> scannedItems = getScannedItems(scannedItemsOf(session, line));
		List<StockItem> stockItems = new StockItemRepository(session).findByLoadUnitOrderByIdAsc(loadUnit);
		if (!stockItems.isEmpty()) {
			for (Reservation reservation : new ReservationRepository(session).findByStockItemIn(stockItems)) {
				reservation.getStockItem().setReservation(reservation);
			}
		}
		updateExistingStockItems(session, loadUnit, stockItems, scannedItems);
	}

	public InventoryOrderLineResponse createPlannedInventoryOrderLine(Long orderId, Integer locationId) {
		return new WithSessionAndTransaction<InventoryOrderLineResponse>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				InventoryOrder order = findStartedOrder(session, orderId);
				Location location = new LocationRepository(session).findById(locationId)
						.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
								"Location was not found: " + locationId));
				if (!(location instanceof StorageLocation)) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
							"Inventory order lines require a storage location.");
				}
				InventoryOrderLineRepository lineRepository = new InventoryOrderLineRepository(session);
				if (lineRepository.existsByInventoryOrderIdAndLocationId(orderId, locationId)) {
					throw new ResponseStatusException(HttpStatus.CONFLICT,
							"This location already has a line in the inventory order.");
				}

				InventoryOrderLine line = new InventoryOrderLine();
				line.setInventoryOrder(order);
				line.setLocation(location);
				line.setLocationName(location.getName());
				line.setStatus(InventoryOrderLineStatus.NEW);
				line.setStockAdjustmentsCalculated(false);
				line.setStockUpdated(false);
				lineRepository.insert(line);
				setReturnValue(toLineResponse(session, line));
			}
		}.run();
	}

	public InventoryOrderLineResponse startInventoryOrderLine(Long orderId, InventoryOrderLineStartRequest request) {
		return new WithSessionAndTransaction<InventoryOrderLineResponse>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				findStartedOrder(session, orderId);
				InventoryOrderLineRepository lineRepository = new InventoryOrderLineRepository(session);
				if (lineRepository.existsByInventoryOrderIdAndStatus(orderId, InventoryOrderLineStatus.STARTED)) {
					throw new ResponseStatusException(HttpStatus.CONFLICT,
							"Finish the started load-unit line before starting another.");
				}
				InventoryOrderLine line = lineRepository.findByInventoryOrderId(orderId).stream()
						.filter(candidate -> candidate.getStatus() == InventoryOrderLineStatus.NEW).findFirst()
						.orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
								"This inventory order has no new lines to scan."));

				// the real entity, not a lazy proxy, so the instanceof checks work
				Location location = Hibernate.unproxy(line.getLocation(), Location.class);
				if (location == null || location instanceof TransitLocation) {
					throw new ResponseStatusException(HttpStatus.CONFLICT,
							"The current inventory line must have a non-transit location.");
				}
				LoadUnit foundLoadUnit = new LoadUnitRepository(session).findByBarcode(request.loadUnitBarcode())
						.orElseGet(() -> createPalletAt(session, request.loadUnitBarcode(), location));
				LoadUnit loadUnit = findEligibleLoadUnit(session, foundLoadUnit.getBarcode());
				if (loadUnit.getLocation().getId() != location.getId()) {
					throw new ResponseStatusException(HttpStatus.CONFLICT, "Load unit " + loadUnit.getBarcode()
							+ " is not at the current line location " + location.getName() + ".");
				}
				if (lineRepository.findByInventoryOrderId(orderId).stream()
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
				setReturnValue(toLineResponse(session, lineRepository.save(line)));
			}
		}.run();
	}

	public InventoryOrderLineResponse saveInventoryOrderLineItems(Long orderId, Long lineId,
			List<ScannedInventoryItemRequest> items) {
		return new WithSessionAndTransaction<InventoryOrderLineResponse>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				InventoryOrderLine line = findStartedLine(session, orderId, lineId);
				InventoryOrderLineItemRepository itemRepository = new InventoryOrderLineItemRepository(session);

				// item-urile scanate inainte se inlocuiesc complet cu cele noi
				itemRepository.findByInventoryOrderLineId(lineId).forEach(itemRepository::delete);
				for (ScannedInventoryItemRequest request : items) {
					InventoryOrderLineItem item = new InventoryOrderLineItem();
					item.setInventoryOrderLine(line);
					item.setArticleBarcode(request.articleBarcode());
					item.setArticleDescription(request.articleDescription());
					item.setPackagingUnitName(request.packagingUnitName());
					item.setQuantity(request.quantity());
					itemRepository.insert(item);
				}
				setReturnValue(toLineResponse(session, line));
			}
		}.run();
	}

	public InventoryOrderLineResponse finishEmptyInventoryOrderLine(Long orderId, Long lineId) {
		return new WithSessionAndTransaction<InventoryOrderLineResponse>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				InventoryOrderLineRepository lineRepository = new InventoryOrderLineRepository(session);
				LoadUnitRepository loadUnitRepository = new LoadUnitRepository(session);
				StockItemRepository stockItemRepository = new StockItemRepository(session);

				InventoryOrder order = findStartedOrder(session, orderId);
				InventoryOrderLine line = lineRepository.findById(lineId)
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

				// the real entity, not a lazy proxy, so instanceof StorageLocation works
				Location location = Hibernate.unproxy(line.getLocation(), Location.class);

				List<LoadUnit> loadUnits = loadUnitRepository.findByLocationOrderByIdAsc(location);
				if (!loadUnits.isEmpty()) {
					line.setLoadUnitBarcode(loadUnits.get(0).getBarcode());
					line.setLoadUnitType(loadUnits.get(0).getType().name());
					line.setLocationName(location.getName());
				}
				Map<ItemKey, Integer> expectedQuantities = getExpectedQuantities(
						getLocationStockItems(session, location));
				StockAdjustmentRepository adjustmentRepository = new StockAdjustmentRepository(session);
				for (Map.Entry<ItemKey, Integer> expected : expectedQuantities.entrySet()) {
					adjustmentRepository.insert(createDifference(session, line, expected.getKey(), null,
							expected.getValue(), 0, -expected.getValue()));
				}
				for (LoadUnit missingLoadUnit : loadUnits) {
					List<StockItem> missingStockItems = stockItemRepository.findByLoadUnitOrderByIdAsc(missingLoadUnit);
					requeueActiveOrders(session, missingStockItems);
					if (!new ReservationRepository(session).findByStockItemIn(missingStockItems).isEmpty()) {
						throw new ResponseStatusException(HttpStatus.CONFLICT,
								"Cannot remove a missing load unit while it still has reservations.");
					}
					missingStockItems.forEach(stockItemRepository::delete);
					for (InventoryOrderLine oldLine : lineRepository.findByLoadUnit(missingLoadUnit)) {
						oldLine.setLoadUnit(null);
					}
					session.flush();

					loadUnitRepository.delete(missingLoadUnit);
				}
				line.setStatus(InventoryOrderLineStatus.FINISHED);
				line.setCompletedAt(LocalDateTime.now());
				line.setStockAdjustmentsCalculated(true);
				setReturnValue(toLineResponse(session, lineRepository.save(line)));
			}
		}.run();
	}

	public InventoryOrderResponse createInventoryOrder(String requestedCode) {
		String code = requestedCode.trim();
		return new WithSessionAndTransaction<InventoryOrderResponse>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				InventoryOrderRepository orderRepository = new InventoryOrderRepository(session);
				if (orderRepository.existsByCode(code)) {
					throw new ResponseStatusException(HttpStatus.CONFLICT,
							"Inventory order code already exists: " + code);
				}
				InventoryOrder order = new InventoryOrder();
				order.setCode(code);
				order.setCreatedAt(LocalDateTime.now());
				order.setReasonMessage("INVENTORY");
				order.setStatus(InventoryOrderStatus.STARTED);
				orderRepository.insert(order);
				setReturnValue(toOrderResponse(session, order));
			}
		}.run();
	}

	public InventoryOrderResponse getStartedInventoryOrder(String code) {
		return new WithSessionAndTransaction<InventoryOrderResponse>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				InventoryOrder order = new InventoryOrderRepository(session)
						.findByCodeAndStatus(code, InventoryOrderStatus.STARTED)
						.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
								"Started inventory order was not found: " + code));
				setReturnValue(toOrderResponse(session, order));
			}
		}.run();
	}

	public InventoryOrderLineResponse finishInventoryOrderLine(Long orderId, Long lineId) {
		return new WithSessionAndTransaction<InventoryOrderLineResponse>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				InventoryOrderLineRepository lineRepository = new InventoryOrderLineRepository(session);
				InventoryOrderLine line = findStartedLine(session, orderId, lineId);
				line.setStatus(InventoryOrderLineStatus.FINISHED);
				line.setCompletedAt(LocalDateTime.now());
				line = lineRepository.save(line);
				if (!line.isStockAdjustmentsCalculated()) {
					createInventoryAdjustments(session, line);
					line.setStockAdjustmentsCalculated(true);
				}
				if (!line.isStockUpdated()) {
					updateInventoryOrderLineStock(session, line);
					line.setStockUpdated(true);
				}
				setReturnValue(toLineResponse(session, lineRepository.save(line)));
			}
		}.run();
	}

	public void finishInventoryOrder(Long orderId) {
		new WithSessionAndTransaction<Void>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				InventoryOrderLineRepository lineRepository = new InventoryOrderLineRepository(session);
				InventoryOrder order = findStartedOrder(session, orderId);
				List<InventoryOrderLine> lines = lineRepository.findByInventoryOrderId(orderId);
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
						createInventoryAdjustments(session, line);
						line.setStockAdjustmentsCalculated(true);
					}
					if (!line.isStockUpdated()) {
						updateInventoryOrderLineStock(session, line);
						line.setStockUpdated(true);
					}
					lineRepository.save(line);
				}

				order.setStatus(InventoryOrderStatus.FINISHED);
				order.setFinishedAt(now);
				new InventoryOrderRepository(session).save(order);
			}
		}.run();
	}

	private record ItemKey(String articleBarcode, String packagingUnitName) {
	}
}