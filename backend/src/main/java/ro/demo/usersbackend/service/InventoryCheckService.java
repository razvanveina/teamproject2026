package ro.demo.usersbackend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.demo.usersbackend.dto.InventoryCheckRequest;
import ro.demo.usersbackend.dto.ScannedInventoryItemRequest;
import ro.demo.usersbackend.entity.Article;
import ro.demo.usersbackend.entity.ArticlePackagingUnitType;
import ro.demo.usersbackend.entity.InventoryCheck;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.PackagingUnitType;
import ro.demo.usersbackend.entity.Reservation;
import ro.demo.usersbackend.entity.RetrievalOrder;
import ro.demo.usersbackend.entity.RetrievalOrderLine;
import ro.demo.usersbackend.entity.RetrievalOrderLineStatus;
import ro.demo.usersbackend.entity.RetrievalOrderStatus;
import ro.demo.usersbackend.entity.StockAdjustment;
import ro.demo.usersbackend.entity.StockAdjustmentReason;
import ro.demo.usersbackend.entity.StockItem;
import ro.demo.usersbackend.repository.ArticlePackagingUnitTypeRepository;
import ro.demo.usersbackend.repository.ArticleRepository;
import ro.demo.usersbackend.repository.InventoryCheckRepository;
import ro.demo.usersbackend.repository.LoadUnitRepository;
import ro.demo.usersbackend.repository.PackagingUnitTypeRepository;
import ro.demo.usersbackend.repository.ReservationRepository;
import ro.demo.usersbackend.repository.StockAdjustmentRepository;
import ro.demo.usersbackend.repository.StockItemRepository;

@Service
@Transactional
public class InventoryCheckService {
	private final InventoryCheckRepository inventoryCheckRepository;
	private final StockAdjustmentRepository inventoryDifferenceRepository;
	private final LoadUnitRepository loadUnitRepository;
	private final StockItemRepository stockItemRepository;
	private final ReservationRepository reservationRepository;
	private final ArticleRepository articleRepository;
	private final PackagingUnitTypeRepository packagingUnitTypeRepository;
	private final ArticlePackagingUnitTypeRepository articlePackagingUnitTypeRepository;
	private static final Logger log = LoggerFactory.getLogger(InventoryCheckService.class);

	public InventoryCheckService(InventoryCheckRepository inventoryCheckRepository,
			StockAdjustmentRepository inventoryDifferenceRepository, LoadUnitRepository loadUnitRepository,
			StockItemRepository stockItemRepository, ReservationRepository reservationRepository,
			ArticleRepository articleRepository, PackagingUnitTypeRepository packagingUnitTypeRepository,
			ArticlePackagingUnitTypeRepository articlePackagingUnitTypeRepository) {
		this.inventoryCheckRepository = inventoryCheckRepository;
		this.inventoryDifferenceRepository = inventoryDifferenceRepository;
		this.loadUnitRepository = loadUnitRepository;
		this.stockItemRepository = stockItemRepository;
		this.reservationRepository = reservationRepository;
		this.articleRepository = articleRepository;
		this.packagingUnitTypeRepository = packagingUnitTypeRepository;
		this.articlePackagingUnitTypeRepository = articlePackagingUnitTypeRepository;
	}

	public InventoryCheck saveInventoryCheck(InventoryCheckRequest request) {
		LoadUnit loadUnit = loadUnitRepository.findByBarcode(request.loadUnitBarcode()).orElse(null);

		InventoryCheck check = new InventoryCheck();
		check.setLoadUnit(loadUnit);
		check.setLoadUnitBarcode(request.loadUnitBarcode());
		check.setLoadUnitType(request.loadUnitType());
		check.setScannedAt(LocalDateTime.now());
		check.setReasonMessage("INVENTORY");

		InventoryCheck savedCheck = inventoryCheckRepository.save(check);

		List<StockItem> stockItems = loadUnit == null ? new ArrayList<>()
				: stockItemRepository.findByLoadUnitOrderByIdAsc(loadUnit);
		if (!stockItems.isEmpty()) {
			for (Reservation reservation : reservationRepository.findByStockItemIn(stockItems)) {
				reservation.getStockItem().setReservation(reservation);
			}
		}

		Map<ItemKey, Integer> expectedQuantities = getExpectedQuantities(stockItems);
		Map<ItemKey, ScannedInventoryItemRequest> scannedItems = getScannedItems(request.items());

		Set<ItemKey> allItems = new LinkedHashSet<>();
		allItems.addAll(expectedQuantities.keySet());
		allItems.addAll(scannedItems.keySet());

		List<StockAdjustment> differences = new ArrayList<>();

		for (ItemKey key : allItems) {
			int expected = expectedQuantities.getOrDefault(key, 0);
			ScannedInventoryItemRequest scanned = scannedItems.get(key);
			int counted = scanned == null ? 0 : scanned.quantity();
			int difference = counted - expected;

			if (difference != 0) {
				differences.add(createDifference(savedCheck, key, scanned, expected, counted, difference));
			}
		}

		inventoryDifferenceRepository.saveAll(differences);

		updateExistingStockItems(loadUnit, stockItems, scannedItems);

		return savedCheck;
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

	private Map<ItemKey, Integer> getExpectedQuantities(LoadUnit loadUnit) {
		Map<ItemKey, Integer> quantities = new LinkedHashMap<>();

		if (loadUnit == null) {
			return quantities;
		}

		for (StockItem stockItem : stockItemRepository.findByLoadUnitOrderByIdAsc(loadUnit)) {
			ArticlePackagingUnitType articlePackaging = stockItem.getArticlePackagingUnitType();

			ItemKey key = new ItemKey(articlePackaging.getArticle().getBarcode(),
					articlePackaging.getPackagingUnitType().getName());

			quantities.merge(key, stockItem.getQuantity(), Integer::sum);
		}

		return quantities;
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

	private StockAdjustment createDifference(InventoryCheck check, ItemKey key, ScannedInventoryItemRequest scanned,
			int expected, int counted, int difference) {
		Article article = articleRepository.findByBarcode(key.articleBarcode()).orElse(null);
		PackagingUnitType packagingUnitType = packagingUnitTypeRepository.findByName(key.packagingUnitName())
				.orElse(null);
		ArticlePackagingUnitType articlePackagingUnitType = findArticlePackagingUnitType(article, packagingUnitType);

		StockAdjustment inventoryDifference = new StockAdjustment();
		inventoryDifference.setInventoryCheck(check);
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