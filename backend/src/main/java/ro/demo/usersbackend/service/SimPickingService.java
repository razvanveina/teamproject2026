package ro.demo.usersbackend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.dto.SimItemResponse;
import ro.demo.usersbackend.dto.SimStationResponse;
import ro.demo.usersbackend.dto.SimToteResponse;
import ro.demo.usersbackend.entity.ArticlePackagingUnitType;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.Reservation;
import ro.demo.usersbackend.entity.RetrievalOrder;
import ro.demo.usersbackend.entity.RetrievalOrderLine;
import ro.demo.usersbackend.entity.RetrievalOrderLineStatus;
import ro.demo.usersbackend.entity.RetrievalOrderStatus;
import ro.demo.usersbackend.entity.StockItem;
import ro.demo.usersbackend.entity.TransportOrder;
import ro.demo.usersbackend.entity.TransportOrderStatus;
import ro.demo.usersbackend.repository.LoadUnitRepository;
import ro.demo.usersbackend.repository.LocationRepository;
import ro.demo.usersbackend.repository.TransportOrderRepository;
import ro.demo.usersbackend.server.mfs.StationControl;
import ro.demo.usersbackend.server.picking.StationChooser;

// statia de picking din simulator: tote-ul sursa sta pe PTTS1, tote-ul tinta pe PTTT1
@Service
public class SimPickingService {
	public static final Logger log = LogManager.getLogger("application");

	public SimPickingService() {
		super();
	}

	public int stationCount() {
		return Constants.PTTS_POINTS.size();
	}

	// ce e acum in statie, sus si jos, si orderul pe care lucreaza statia
	public SimStationResponse findStation(int station) {
		return new WithSessionAndTransaction<SimStationResponse>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				LoadUnit source = findToteAt(session, stationPoint(Constants.PTTS_POINTS, station));
				LoadUnit target = findToteAt(session, stationPoint(Constants.PTTT_POINTS, station));

				String orderId = null;
				if (source != null && target != null) {
					RetrievalOrder order = chooseOrder(source, target);
					if (order != null) {
						orderId = order.getOrderId();
					}
				}
				setReturnValue(new SimStationResponse(toResponse(source), toResponse(target), orderId));
			}
		}.run();
	}

	// tote-ul care are locatia egala cu punctul; null daca nu e niciunul
	private LoadUnit findToteAt(Session session, String point) {
		List<LoadUnit> totes = new LoadUnitRepository(session).findByLocationName(point);
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
	// (acelasi order pe care il arata si ecranul)
	// sursa primeste un TO dupa ce a ramas in ea (routeSource), apoi release
	// tinta primeste release (spre OUT) doar cand orderul e complet
	public String confirm(int station, String sourceBarcode, String targetBarcode) {
		return new WithSessionAndTransaction<String>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				String ptts = stationPoint(Constants.PTTS_POINTS, station);
				String pttt = stationPoint(Constants.PTTT_POINTS, station);
				LoadUnit source = findToteAt(session, sourceBarcode, ptts);
				LoadUnit target = findToteAt(session, targetBarcode, pttt);

				RetrievalOrder order = chooseOrder(source, target);
				if (order == null) {
					routeSource(session, source, ptts);
					release(ptts);
					setReturnValue("Tote " + sourceBarcode + " has nothing to pick here, it goes on");
					return;
				}

				int moved = 0;
				for (StockItem item : source.getStockItems()) {
					if (item.getReservation() != null && item.getReservation().getRetrievalOrderLine().getOrder()
							.getOrderId().equals(order.getOrderId())) {
						item.setLoadUnit(target);
						moved++;
						log.info("Sim picking: stock item {} moved from {} to {} for order {}", item.getId(),
								sourceBarcode, targetBarcode, order.getOrderId());
					}
				}

				if (moved == 0) {
					routeSource(session, source, ptts);
					release(ptts);
					setReturnValue(
							"Tote " + sourceBarcode + " has nothing for order " + order.getOrderId() + ", it goes on");
					return;
				}

				updateStatus(order, target);
				routeSource(session, source, ptts);
				release(ptts);
				if (order.getStatus() == RetrievalOrderStatus.PICKED) {
					release(pttt);
					setReturnValue("Order " + order.getOrderId() + " is complete, tote " + targetBarcode + " goes to "
							+ Constants.OUT);
					return;
				}
				setReturnValue("Moved to " + targetBarcode + ", the order " + order.getOrderId() + " needs more totes");
			}
		}.run();
	}

	private String stationPoint(List<String> points, int station) {
		if (station < 1 || station > points.size()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No picking zone " + station);
		}
		return points.get(station - 1);
	}

	// TO-ul tote-ului sursa, dupa ce a ramas in el:
	// gol -> PTTT1 (prin EMPTY), stoc pentru un order automat -> PTTS1 (inca o
	// tura),
	// altfel (stoc liber sau pentru un order manual) -> TPT1, de unde merge inapoi
	// in picking
	// e creat direct ACTIVE: tote-ul e deja pe banda, il inchide PointHandler cand
	// ajunge
	private void routeSource(Session session, LoadUnit source, String from) {
		boolean empty = true;
		Long automaticOrder = null;
		// source.getStockItems() inca vede ce tocmai am mutat in tinta, deci numaram
		// doar ce e tot in sursa
		for (StockItem item : source.getStockItems()) {
			if (item.getLoadUnit().getBarcode().equals(source.getBarcode())) {
				empty = false;
				if (item.getReservation() != null && automaticOrder == null) {
					RetrievalOrder order = item.getReservation().getRetrievalOrderLine().getOrder();
					if (order.isAutomatic()) {
						automaticOrder = order.getId();
					}
				}
			}
		}

		Long orderId = automaticOrder;
		String barcode = source.getBarcode();
		String next;
		if (empty) {
			next = chooseStation(session, StationChooser::freeTargetStation);
		} else if (orderId != null) {
			next = chooseStation(session, s -> StationChooser.sourceStationFor(s, orderId, barcode));
		} else {
			next = Constants.TPT1;
		}

		if (next == null) {
			log.info("Sim picking: empty tote {} has no free target zone, it goes on without a TO", barcode);
			return;
		}
		new TransportOrderRepository(session).insert(new TransportOrder(location(session, from),
				location(session, next), TransportOrderStatus.ACTIVE, source));
		log.info("Sim picking: tote {} goes from {} to {}", barcode, from, next);
	}

	private String chooseStation(Session session, Function<Session, String> choice) {
		try {
			return choice.apply(session);
		} catch (RuntimeException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
					"Cannot choose a picking zone: " + e.getMessage());
		}
	}

	private Location location(Session session, String name) {
		return new LocationRepository(session).findByName(name)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
						"Location " + name + " is missing"));
	}

	// ii spunem simulatorului sa dea drumul tote-ului din punct
	// daca nu merge, exceptia anuleaza si mutarea stocului si TO-ul (tranzactia se
	// intoarce)
	private void release(String point) {
		try {
			StationControl.getInstance().release(point);
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
					"Cannot release " + point + ": " + e.getMessage());
		}
		log.info("Sim picking: {} released", point);
	}

	// tote-ul cu barcode-ul dat trebuie sa fie chiar in punctul asteptat
	private LoadUnit findToteAt(Session session, String barcode, String point) {
		LoadUnit tote = new LoadUnitRepository(session).findByBarcode(barcode)
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