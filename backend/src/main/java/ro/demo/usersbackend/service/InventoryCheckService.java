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
import ro.demo.usersbackend.entity.StockAdjustment;
import ro.demo.usersbackend.entity.StockItem;
import ro.demo.usersbackend.repository.ArticlePackagingUnitTypeRepository;
import ro.demo.usersbackend.repository.ArticleRepository;
import ro.demo.usersbackend.repository.InventoryCheckRepository;
import ro.demo.usersbackend.repository.LoadUnitRepository;
import ro.demo.usersbackend.repository.PackagingUnitTypeRepository;
import ro.demo.usersbackend.repository.StockAdjustmentRepository;
import ro.demo.usersbackend.repository.StockItemRepository;

@Service
@Transactional
public class InventoryCheckService {
	private final InventoryCheckRepository inventoryCheckRepository;
	private final StockAdjustmentRepository inventoryDifferenceRepository;
	private final LoadUnitRepository loadUnitRepository;
	private final StockItemRepository stockItemRepository;
	private final ArticleRepository articleRepository;
	private final PackagingUnitTypeRepository packagingUnitTypeRepository;
	private final ArticlePackagingUnitTypeRepository articlePackagingUnitTypeRepository;
	private static final Logger log = LoggerFactory.getLogger(InventoryCheckService.class);

	public InventoryCheckService(InventoryCheckRepository inventoryCheckRepository,
			StockAdjustmentRepository inventoryDifferenceRepository, LoadUnitRepository loadUnitRepository,
			StockItemRepository stockItemRepository, ArticleRepository articleRepository,
			PackagingUnitTypeRepository packagingUnitTypeRepository,
			ArticlePackagingUnitTypeRepository articlePackagingUnitTypeRepository) {
		this.inventoryCheckRepository = inventoryCheckRepository;
		this.inventoryDifferenceRepository = inventoryDifferenceRepository;
		this.loadUnitRepository = loadUnitRepository;
		this.stockItemRepository = stockItemRepository;
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

		InventoryCheck savedCheck = inventoryCheckRepository.save(check);

		List<StockItem> stockItems = loadUnit == null ? new ArrayList<>()
				: stockItemRepository.findByLoadUnitOrderByIdAsc(loadUnit);

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

		updateExistingStockItems(stockItems, scannedItems);

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

	private void updateExistingStockItems(List<StockItem> stockItems,
			Map<ItemKey, ScannedInventoryItemRequest> scannedItems) {
		if (stockItems.isEmpty()) {
			if (!scannedItems.isEmpty()) {
				log.warn("Cannot update StockItems: load unit was not found or has no existing stock.");
			}
			return;
		}

		Map<ItemKey, List<StockItem>> itemsByKey = new LinkedHashMap<>();
		for (StockItem stockItem : stockItems) {
			itemsByKey.computeIfAbsent(getItemKey(stockItem), key -> new ArrayList<>()).add(stockItem);
		}

		for (Map.Entry<ItemKey, ScannedInventoryItemRequest> entry : scannedItems.entrySet()) {
			if (!itemsByKey.containsKey(entry.getKey()) && entry.getValue().quantity() > 0) {
				log.warn("Skipping stock update for unknown article/package: {} / {}", entry.getKey().articleBarcode(),
						entry.getKey().packagingUnitName());
			}
		}

		for (Map.Entry<ItemKey, List<StockItem>> entry : itemsByKey.entrySet()) {
			ItemKey key = entry.getKey();
			List<StockItem> existingItems = entry.getValue();
			ScannedInventoryItemRequest scanned = scannedItems.get(key);
			int countedQuantity = scanned == null ? 0 : scanned.quantity();

			List<StockItem> unreservedItems = new ArrayList<>();
			int reservedQuantity = 0;

			for (StockItem stockItem : existingItems) {
				if (stockItem.getReservation() == null) {
					unreservedItems.add(stockItem);
				} else {
					reservedQuantity += stockItem.getQuantity();
				}
			}

			if (countedQuantity < reservedQuantity) {
				log.warn(
						"Skipping stock update for article/package {} / {}: counted quantity {} is below reserved quantity {}.",
						key.articleBarcode(), key.packagingUnitName(), countedQuantity, reservedQuantity);
				continue;
			}

			int unreservedCountedQuantity = countedQuantity - reservedQuantity;

			if (unreservedItems.isEmpty()) {
				if (unreservedCountedQuantity > 0) {
					log.warn(
							"Skipping surplus stock update for reserved article/package {} / {}: no existing unreserved StockItem to update.",
							key.articleBarcode(), key.packagingUnitName());
				}
				continue;
			}

			StockItem itemToKeep = unreservedItems.get(0);

			if (unreservedCountedQuantity == 0) {
				for (StockItem stockItem : unreservedItems) {
					stockItemRepository.delete(stockItem);
				}
				continue;
			}

			itemToKeep.setQuantity(unreservedCountedQuantity);
			stockItemRepository.save(itemToKeep);

			for (int i = 1; i < unreservedItems.size(); i++) {
				stockItemRepository.delete(unreservedItems.get(i));
			}
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

	private record ItemKey(String articleBarcode, String packagingUnitName) {
	}
}