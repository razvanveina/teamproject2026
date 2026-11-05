package ro.demo.usersbackend.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import ro.demo.usersbackend.dto.ScannedPackage;
import ro.demo.usersbackend.entity.Article;
import ro.demo.usersbackend.entity.ArticlePackagingUnitType;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.LoadUnitType;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.ReceivingOrder;
import ro.demo.usersbackend.entity.ReceivingOrderLine;
import ro.demo.usersbackend.entity.ReceivingOrderLineStatus;
import ro.demo.usersbackend.entity.StockItem;
import ro.demo.usersbackend.repository.ArticlePackagingUnitTypeRepository;
import ro.demo.usersbackend.repository.LoadUnitRepository;
import ro.demo.usersbackend.repository.LocationRepository;
import ro.demo.usersbackend.repository.StockItemRepository;

@Service
public class LoadUnitService {
	@PersistenceContext
	private EntityManager entityManager;

	private final StockItemRepository stockItemRepository;
	private final LoadUnitRepository loadUnitRepository;
	private final LocationRepository locationRepository;
	private final ArticlePackagingUnitTypeRepository articlePackagingUnitTypeRepository;

	public LoadUnitService(EntityManager entityManager, StockItemRepository stockItemRepository,
			LoadUnitRepository loadUnitRepository, LocationRepository locationRepository,
			ArticlePackagingUnitTypeRepository articlePackagingUnitTypeRepository) {
		super();
		this.entityManager = entityManager;
		this.stockItemRepository = stockItemRepository;
		this.loadUnitRepository = loadUnitRepository;
		this.locationRepository = locationRepository;
		this.articlePackagingUnitTypeRepository = articlePackagingUnitTypeRepository;
	}

	public LoadUnit createLoadUnit(List<ScannedPackage> scannedPackages) {
		Location in = locationRepository.findByName("IN")
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot find a IN location"));

		Map<Article, Integer> piecesPerArticle = new LinkedHashMap<>();
		for (ScannedPackage scanned : scannedPackages) {
			ArticlePackagingUnitType pack = articlePackagingUnitTypeRepository.findByBarcode(scanned.packageBarcode())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
							"Could not find package with barcode " + scanned.packageBarcode()));

			int pieces = scanned.packages() * pack.getPackagingUnitType().getQuantity();
			int total = piecesPerArticle.getOrDefault(pack.getArticle(), 0);
			piecesPerArticle.put(pack.getArticle(), total + pieces);

		}
		LoadUnit loadUnit = new LoadUnit();
		loadUnit.setBarcode(generateLoadUnitBarcode());
		loadUnit.setType(LoadUnitType.PALLET);
		loadUnit.setLocation(in);
		loadUnitRepository.save(loadUnit);

		for (Map.Entry<Article, Integer> entry : piecesPerArticle.entrySet()) {
			ArticlePackagingUnitType piece = articlePackagingUnitTypeRepository
					.findByBarcode(entry.getKey().getBarcode())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
							"Cannot find the PIECE package of article " + entry.getKey().getBarcode()));

			stockItemRepository.save(new StockItem(piece, entry.getValue(), loadUnit));
		}
		return loadUnit;
	}

	private String generateLoadUnitBarcode() {
		Number result = (Number) entityManager.createNativeQuery("SELECT LOADUNIT_SEQUENCE.NEXTVAL FROM DUAL")
				.getSingleResult();

		return String.format("%05d", result.longValue());
	}
}
//order by x