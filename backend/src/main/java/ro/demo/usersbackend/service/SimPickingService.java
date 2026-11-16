package ro.demo.usersbackend.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ro.demo.usersbackend.dto.SimItemResponse;
import ro.demo.usersbackend.dto.SimStationResponse;
import ro.demo.usersbackend.dto.SimToteResponse;
import ro.demo.usersbackend.entity.ArticlePackagingUnitType;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.Reservation;
import ro.demo.usersbackend.entity.RetrievalOrder;
import ro.demo.usersbackend.entity.RetrievalOrderLine;
import ro.demo.usersbackend.entity.RetrievalOrderLineStatus;
import ro.demo.usersbackend.entity.RetrievalOrderStatus;
import ro.demo.usersbackend.entity.StockItem;
import ro.demo.usersbackend.repository.LoadUnitRepository;
import ro.demo.usersbackend.server.mfs.StationControl;

// statia de picking din simulator: tote-ul sursa sta pe PTTS1, tote-ul tinta pe PTTT1
@Service
@Transactional
public class SimPickingService {
	public static final Logger log = LogManager.getLogger("application");

	private final LoadUnitRepository loadUnitRepository;

	public SimPickingService(LoadUnitRepository loadUnitRepository) {
		this.loadUnitRepository = loadUnitRepository;
	}

	// ce e acum in statie, sus si jos, si orderul pe care lucreaza statia
	@Transactional(readOnly = true)
	public SimStationResponse findStation() {
		LoadUnit source = findToteAt("PTTS1");
		LoadUnit target = findToteAt("PTTT1");

		String orderId = null;
		if (source != null && target != null) {
			RetrievalOrder order = chooseOrder(source, target);
			if (order != null) {
				orderId = order.getOrderId();
			}
		}
		return new SimStationResponse(toResponse(source), toResponse(target), orderId);
	}

	// tote-ul care are locatia egala cu punctul; null daca nu e niciunul
	private LoadUnit findToteAt(String point) {
		List<LoadUnit> totes = loadUnitRepository.findByLocationName(point);
		if (totes.isEmpty()) {
			return null;
		}
		if (totes.size() > 1) {
			log.warn("Sim picking: {} totes on {}, showing {}", totes.size(), point, totes.get(0).getBarcode());
		}
		return totes.get(0);
	}

	// tote-ul cu tot ce e in el, pentru ecran
	private SimToteResponse toResponse(LoadUnit tote) {
		if (tote == null) {
			return null;
		}
		List<SimItemResponse> items = new ArrayList<>();
		for (StockItem item : tote.getStockItems()) {
			ArticlePackagingUnitType packaging = item.getArticlePackagingUnitType();
			String orderId = null;
			if (item.getReservation() != null) {
				orderId = item.getReservation().getRetrievalOrderLine().getOrder().getOrderId();
			}
			items.add(new SimItemResponse(packaging.getArticle().getDescription(), packaging.getBarcode(),
					item.getQuantity(), packaging.getPackagingUnitType().getQuantity(), orderId));
		}
		return new SimToteResponse(tote.getBarcode(), items);
	}

	// muncitorul a mutat marfa din tote-ul de sus in cel de jos si a apasat Confirm
	// mutam in tinta doar marfa rezervata pentru orderul ales de chooseOrder
	// (acelasi order pe care il arata si ecranul), apoi sursa primeste release
	// (drumul il decide DP2)
	// tinta primeste release (spre OUT) doar cand orderul e complet
	public String confirm(String sourceBarcode, String targetBarcode) {
		LoadUnit source = findToteAt(sourceBarcode, "PTTS1");
		LoadUnit target = findToteAt(targetBarcode, "PTTT1");

		RetrievalOrder order = chooseOrder(source, target);
		if (order == null) {
			release("PTTS1");
			return "Tote " + sourceBarcode + " has nothing to pick here, it goes on";
		}

		int moved = 0;
		for (StockItem item : source.getStockItems()) {
			if (item.getReservation() != null && item.getReservation().getRetrievalOrderLine().getOrder().getOrderId()
					.equals(order.getOrderId())) {
				item.setLoadUnit(target);
				moved++;
				log.info("Sim picking: stock item {} moved from {} to {} for order {}", item.getId(), sourceBarcode,
						targetBarcode, order.getOrderId());
			}
		}

		if (moved == 0) {
			release("PTTS1");
			return "Tote " + sourceBarcode + " has nothing for order " + order.getOrderId() + ", it goes on";
		}

		updateStatus(order, target);
		release("PTTS1");
		if (order.getStatus() == RetrievalOrderStatus.PICKED) {
			release("PTTT1");
			return "Order " + order.getOrderId() + " is complete, tote " + targetBarcode + " goes to OUT";
		}
		return "Moved to " + targetBarcode + ", the order " + order.getOrderId() + " needs more totes";
	}

	// ii spunem simulatorului sa dea drumul tote-ului din punct
	// daca nu merge, exceptia anuleaza si mutarea stocului (tranzactia se intoarce)
	private void release(String point) {
		try {
			StationControl.release(point);
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
					"Cannot release " + point + ": " + e.getMessage());
		}
		log.info("Sim picking: {} released", point);
	}

	// tote-ul cu barcode-ul dat trebuie sa fie chiar in punctul asteptat
	private LoadUnit findToteAt(String barcode, String point) {
		LoadUnit tote = loadUnitRepository.findByBarcode(barcode)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot find tote " + barcode));
		if (!tote.getLocation().getName().equals(point)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Tote " + barcode + " is not on " + point + " anymore, wait for the screen to refresh");
		}
		return tote;
	}

	// orderul pentru care e rezervata marfa din tote (primul stock item rezervat);
	// null daca nu e nimic rezervat
	private RetrievalOrder findOrder(LoadUnit tote) {
		for (StockItem item : tote.getStockItems()) {
			if (item.getReservation() != null) {
				return item.getReservation().getRetrievalOrderLine().getOrder();
			}
		}
		return null;
	}

	// orderul pe care lucreaza statia: al tintei daca are deja ceva in ea,
	// altfel primul order automat din sursa; orderele manuale nu se iau aici
	// null daca nu e nimic de luat
	private RetrievalOrder chooseOrder(LoadUnit source, LoadUnit target) {
		RetrievalOrder targetOrder = findOrder(target);
		if (targetOrder != null) {
			return targetOrder;
		}
		for (StockItem item : source.getStockItems()) {
			if (item.getReservation() != null) {
				RetrievalOrder order = item.getReservation().getRetrievalOrderLine().getOrder();
				if (order.isAutomatic()) {
					return order;
				}
			}
		}
		return null;
	}

	// linia e PICKED cand are toata cantitatea in tote-ul tinta, STARTED cand are
	// doar o parte
	// orderul e PICKED cand toate liniile sunt PICKED, altfel STARTED
	private void updateStatus(RetrievalOrder order, LoadUnit target) {
		boolean complete = true;
		for (RetrievalOrderLine line : order.getLines()) {
			int pieces = piecesIn(line, target);
			if (pieces >= line.getQuantity()) {
				line.setStatus(RetrievalOrderLineStatus.PICKED);
			} else {
				complete = false;
				if (pieces > 0) {
					line.setStatus(RetrievalOrderLineStatus.STARTED);
				}
			}
		}
		if (complete) {
			order.setStatus(RetrievalOrderStatus.PICKED);
		} else {
			order.setStatus(RetrievalOrderStatus.STARTED);
		}
	}

	// cate bucati rezervate pentru linie sunt deja in tote-ul tinta
	// mergem prin rezervari si nu prin target.getStockItems(), pentru ca lista aia
	// nu vede mutarile facute acum
	private int piecesIn(RetrievalOrderLine line, LoadUnit target) {
		int pieces = 0;
		for (Reservation reservation : line.getReservations()) {
			StockItem item = reservation.getStockItem();
			if (item.getLoadUnit().getBarcode().equals(target.getBarcode())) {
				pieces = pieces + item.getTotalQuantity();
			}
		}
		return pieces;
	}
}