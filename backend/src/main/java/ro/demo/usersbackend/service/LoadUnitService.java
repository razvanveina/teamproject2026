package ro.demo.usersbackend.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import ro.demo.usersbackend.entity.Article;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.LoadUnitType;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.ReceivingOrder;
import ro.demo.usersbackend.entity.ReceivingOrderLine;
import ro.demo.usersbackend.entity.StockItem;
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

	public LoadUnitService(StockItemRepository stockItemRepository, LoadUnitRepository loadUnitRepository,
			LocationRepository locationRepository) {
		super();
		this.stockItemRepository = stockItemRepository;
		this.loadUnitRepository = loadUnitRepository;
		this.locationRepository = locationRepository;
	}

	public LoadUnit createLoadUnit(ReceivingOrder order) {
		// TODO throw error if order not finished
		Location in = locationRepository.findByName("IN")
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot find a IN location"));

		LoadUnit loadUnit = new LoadUnit();
		List<ReceivingOrderLine> lines = order.getLines();
		List<StockItem> items = new ArrayList<>();

		loadUnit.setBarcode(generateLoadUnitBarcode());
		loadUnit.setType(LoadUnitType.PALLET);
		loadUnit.setLocation(in);

		loadUnitRepository.save(loadUnit);

		for (ReceivingOrderLine line : lines) {
			Article article = line.getArticle();
			StockItem si = new StockItem(article, line.getQuantity(), loadUnit, article.getPackagingUnitTypes().get(0));
			stockItemRepository.save(si);
			items.add(si);
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