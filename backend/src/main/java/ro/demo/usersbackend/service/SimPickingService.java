package ro.demo.usersbackend.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.demo.usersbackend.dto.SimItemResponse;
import ro.demo.usersbackend.dto.SimStationResponse;
import ro.demo.usersbackend.dto.SimToteResponse;
import ro.demo.usersbackend.entity.ArticlePackagingUnitType;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.StockItem;
import ro.demo.usersbackend.repository.LoadUnitRepository;

// statia de picking din simulator: tote-ul sursa sta pe PTTS1, tote-ul tinta pe PTTT1
@Service
@Transactional
public class SimPickingService {
	public static final Logger log = LogManager.getLogger("application");

	private final LoadUnitRepository loadUnitRepository;

	public SimPickingService(LoadUnitRepository loadUnitRepository) {
		this.loadUnitRepository = loadUnitRepository;
	}

	@Transactional(readOnly = true)
	public SimStationResponse findStation() {
		return new SimStationResponse(findTote("PTTS1"), findTote("PTTT1"));
	}

	private SimToteResponse findTote(String point) {
		List<LoadUnit> totes = loadUnitRepository.findByLocationName(point);
		if (totes.isEmpty()) {
			return null;
		}

		LoadUnit tote = totes.get(0);

		List<SimItemResponse> items = new ArrayList<>();
		for (StockItem item : tote.getStockItems()) {
			ArticlePackagingUnitType apu = item.getArticlePackagingUnitType();

			String orderId = null;
			if (item.getReservation() != null) {
				orderId = item.getReservation().getRetrievalOrderLine().getOrder().getOrderId();
			}

			items.add(new SimItemResponse(apu.getArticle().getBarcode(), apu.getPackagingUnitType().getName(),
					item.getQuantity(), apu.getPackagingUnitType().getQuantity(), orderId));
		}
		return new SimToteResponse(tote.getBarcode(), items);
	}
}
