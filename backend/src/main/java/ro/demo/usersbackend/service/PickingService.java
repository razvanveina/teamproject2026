package ro.demo.usersbackend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.dto.PickingLineResponse;
import ro.demo.usersbackend.dto.PickingOrderResponse;
import ro.demo.usersbackend.dto.PickingStepResponse;
import ro.demo.usersbackend.entity.ArticlePackagingUnitType;
import ro.demo.usersbackend.entity.InventoryOrder;
import ro.demo.usersbackend.entity.InventoryOrderLine;
import ro.demo.usersbackend.entity.InventoryOrderLineStatus;
import ro.demo.usersbackend.entity.InventoryOrderStatus;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.LoadUnitType;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.Reservation;
import ro.demo.usersbackend.entity.RetrievalOrder;
import ro.demo.usersbackend.entity.RetrievalOrderLine;
import ro.demo.usersbackend.entity.RetrievalOrderLineStatus;
import ro.demo.usersbackend.entity.RetrievalOrderStatus;
import ro.demo.usersbackend.entity.StockAdjustment;
import ro.demo.usersbackend.entity.StockAdjustmentReason;
import ro.demo.usersbackend.entity.StockItem;
import ro.demo.usersbackend.repository.InventoryOrderLineRepository;
import ro.demo.usersbackend.repository.InventoryOrderRepository;
import ro.demo.usersbackend.repository.LoadUnitRepository;
import ro.demo.usersbackend.repository.LocationRepository;
import ro.demo.usersbackend.repository.ReservationRepository;
import ro.demo.usersbackend.repository.RetrievalOrderLineRepository;
import ro.demo.usersbackend.repository.RetrievalOrderRepository;
import ro.demo.usersbackend.repository.StockAdjustmentRepository;
import ro.demo.usersbackend.repository.StockItemRepository;

// toata logica statiei de picking, pe entitati
@Service
public class PickingService {
	public static final Logger log = LogManager.getLogger("application");

	private final TransportOrderService transportOrderService;

	public PickingService(TransportOrderService transportOrderService) {
		super();
		this.transportOrderService = transportOrderService;
	}

	// intai orderul deja luat de user; altfel il luam pe primul liber si il blocam
	// pentru el (pickedBy)
	// ordinea: STARTED apoi ACTIVE, cel mai vechi primul; doar manuale si cu toata
	// marfa in picking
	public PickingOrderResponse findNextOrder(String user) {
		try {
			return new WithSessionAndTransaction<PickingOrderResponse>() {
				@Override
				protected void executeBusinessLogic(Session session) {
					List<RetrievalOrder> orders = findReady(session, user);
					if (orders.isEmpty()) {
						setReturnValue(null);
						return;
					}
					for (RetrievalOrder order : orders) {
						if (user.equals(order.getPickedBy())) {
							setReturnValue(toResponse(order));
							return;
						}
					}
					RetrievalOrder order = orders.get(0);
					order.setPickedBy(user);
					log.info("Picking: order {} is now taken by {}", order.getOrderId(), user);
					setReturnValue(toResponse(order));
				}
			}.run();
		} catch (RuntimeException e) {
			if (e.getCause() instanceof ResponseStatusException) {
				throw (ResponseStatusException) e.getCause();
			}
			throw e;
		}
	}

	// toate orderele gata de picking, pentru lista din butonul Select order
	public List<PickingOrderResponse> findReadyOrders(String user) {
		try {
			return new WithSessionAndTransaction<List<PickingOrderResponse>>() {
				@Override
				protected void executeBusinessLogic(Session session) {
					List<PickingOrderResponse> ready = new ArrayList<>();
					for (RetrievalOrder order : findReady(session, user)) {
						ready.add(toResponse(order));
					}
					setReturnValue(ready);
				}
			}.run();
		} catch (RuntimeException e) {
			if (e.getCause() instanceof ResponseStatusException) {
				throw (ResponseStatusException) e.getCause();
			}
			throw e;
		}
	}

	// orderele gata de picking, fara cele luate de alt user
	private List<RetrievalOrder> findReady(Session session, String user) {
		RetrievalOrderRepository retrievalOrders = new RetrievalOrderRepository(session);
		List<RetrievalOrder> orders = new ArrayList<>(
				retrievalOrders.findByStatusAndAutomaticFalseOrderByIdAsc(RetrievalOrderStatus.STARTED));
		orders.addAll(retrievalOrders.findByStatusAndAutomaticFalseOrderByIdAsc(RetrievalOrderStatus.ACTIVE));

		List<RetrievalOrder> ready = new ArrayList<>();
		for (RetrievalOrder order : orders) {
			boolean takenByOther = order.getPickedBy() != null && !order.getPickedBy().equals(user);
			if (!takenByOther && isInPicking(order)) {
				ready.add(order);
			}
		}
		return ready;
	}

	// userul a ales un order din Select order: devine al lui
	// orderele lui neincepute se elibereaza, ca sa le poata lua altcineva
	public void takeOrder(String orderId, String user) {
		try {
			new WithSessionAndTransaction<Void>() {
				@Override
				protected void executeBusinessLogic(Session session) {
					RetrievalOrder order = findOrder(session, orderId);
					if (order.getPickedBy() != null && !order.getPickedBy().equals(user)) {
						throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
								"Order " + orderId + " is already taken by " + order.getPickedBy());
					}
					releaseOrders(session, user);
					order.setPickedBy(user);
					log.info("Picking: order {} is now taken by {}", orderId, user);
				}
			}.run();
		} catch (RuntimeException e) {
			if (e.getCause() instanceof ResponseStatusException) {
				throw (ResponseStatusException) e.getCause();
			}
			throw e;
		}
	}

	// elibereaza orderele userului la care n-a scanat inca cutia (le-a vazut doar
	// pe ecran)
	public void releaseOrders(String user) {
		try {
			new WithSessionAndTransaction<Void>() {
				@Override
				protected void executeBusinessLogic(Session session) {
					releaseOrders(session, user);
				}
			}.run();
		} catch (RuntimeException e) {
			if (e.getCause() instanceof ResponseStatusException) {
				throw (ResponseStatusException) e.getCause();
			}
			throw e;
		}
	}

	private void releaseOrders(Session session, String user) {
		for (RetrievalOrder order : new RetrievalOrderRepository(session).findByPickedBy(user)) {
			if (!hasBox(order) && order.getStatus() != RetrievalOrderStatus.PICKED) {
				order.setPickedBy(null);
				log.info("Picking: order {} is free again", order.getOrderId());
			}
		}
	}

	// orderul are deja o cutie daca macar un stock item rezervat e intr-un CARTON
	private boolean hasBox(RetrievalOrder order) {
		for (RetrievalOrderLine line : order.getLines()) {
			for (Reservation reservation : line.getReservations()) {
				if (reservation.getStockItem().getLoadUnit().getType() == LoadUnitType.CARTON) {
					return true;
				}
			}
		}
		return false;
	}

	public PickingOrderResponse findOrderDetails(String orderId) {
		try {
			return new WithSessionAndTransaction<PickingOrderResponse>() {
				@Override
				protected void executeBusinessLogic(Session session) {
					setReturnValue(toResponse(findOrder(session, orderId)));
				}
			}.run();
		} catch (RuntimeException e) {
			if (e.getCause() instanceof ResponseStatusException) {
				throw (ResponseStatusException) e.getCause();
			}
			throw e;
		}
	}

	// muncitorul a scanat cutia: o cream ca load unit CARTON in zona PICKING
	// tot acum orderul devine al lui (pickedBy), daca nu l-a luat deja altcineva
	public String createBox(String orderId, String barcode, String user) {
		try {
			return new WithSessionAndTransaction<String>() {
				@Override
				protected void executeBusinessLogic(Session session) {
					LoadUnitRepository loadUnits = new LoadUnitRepository(session);
					if (loadUnits.findByBarcode(barcode).isPresent()) {
						throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Box " + barcode + " already exists");
					}
					RetrievalOrder order = findOrder(session, orderId);
					if (order.getPickedBy() != null && !order.getPickedBy().equals(user)) {
						throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
								"Order " + orderId + " is already taken by " + order.getPickedBy());
					}
					order.setPickedBy(user);

					LoadUnit box = new LoadUnit();
					box.setBarcode(barcode);
					box.setType(LoadUnitType.CARTON);
					Location picking = new LocationRepository(session).findByName(Constants.PICKING)
							.orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
									"Location " + Constants.PICKING + " is missing"));
					box.setLocation(picking);
					loadUnits.save(box);
					order.setStatus(RetrievalOrderStatus.STARTED);
					log.info("Picking: box {} created for order {}", barcode, orderId);
					setReturnValue("Box " + barcode + " ready");
				}
			}.run();
		} catch (RuntimeException e) {
			if (e.getCause() instanceof ResponseStatusException) {
				throw (ResponseStatusException) e.getCause();
			}
			throw e;
		}
	}

	// muncitorul a apasat confirm: stock itemul din pasul de pe ecran intra in
	// cutie
	// il luam dupa id, pentru ca acelasi barcode poate fi in mai multe locatii
	// linia devine PICKED cand e toata in cutie, altfel STARTED (are doar o parte
	// in cutie)
	public void moveItemToBox(Long stockItemId, String boxBarcode) {
		try {
			new WithSessionAndTransaction<Void>() {
				@Override
				protected void executeBusinessLogic(Session session) {
					moveItemToBox(session, stockItemId, boxBarcode);
				}
			}.run();
		} catch (RuntimeException e) {
			if (e.getCause() instanceof ResponseStatusException) {
				throw (ResponseStatusException) e.getCause();
			}
			throw e;
		}
	}

	private void moveItemToBox(Session session, Long stockItemId, String boxBarcode) {
		LoadUnit box = findBox(session, boxBarcode);
		StockItem item = findStockItem(session, stockItemId);
		LoadUnit tote = item.getLoadUnit();
		item.setLoadUnit(box);

		RetrievalOrderLine line = item.getReservation().getRetrievalOrderLine();
		if (isLineInBox(line)) {
			line.setStatus(RetrievalOrderLineStatus.PICKED);
		} else {
			line.setStatus(RetrievalOrderLineStatus.STARTED);
		}
		log.info("Picking: stock item {} ({}) moved into box {}", stockItemId, line.getArticle().getDescription(),
				boxBarcode);

		sendToteIfEmpty(session, tote);
	}

	// muncitorul a apasat Error: a gasit mai putin decat scrie pe ecran (marfa
	// stricata sau lipsa)
	// intai salvam diferenta ca stock adjustment, la fel ca la inventar, apoi
	// stocul scade
	// ce a gasit intra in cutie, restul dispare din baza de date
	// linia ramane deschisa, iar orderul trece pe NEW ca rezervarea sa caute doar
	// partea lipsa
	public void reportError(Long stockItemId, String boxBarcode, String reason, int found) {
		try {
			new WithSessionAndTransaction<Void>() {
				@Override
				protected void executeBusinessLogic(Session session) {
					StockItem item = findStockItem(session, stockItemId);
					if (found < 0 || found >= item.getQuantity()) {
						throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
								"Found quantity must be less than " + item.getQuantity());
					}
					LoadUnit tote = item.getLoadUnit();
					RetrievalOrder order = item.getReservation().getRetrievalOrderLine().getOrder();
					saveStockAdjustment(session, item, found, reason);
					log.warn("Picking: error {} for stock item {} ({}) in {}: found {} of {}", reason, stockItemId,
							item.getArticlePackagingUnitType().getArticle().getDescription(), tote.getBarcode(), found,
							item.getQuantity());

					if (found > 0) {
						item.setQuantity(found);
						moveItemToBox(session, stockItemId, boxBarcode);
					} else {
						Reservation reservation = item.getReservation();
						reservation.getRetrievalOrderLine().getReservations().remove(reservation);
						item.setReservation(null);
						new ReservationRepository(session).delete(reservation);
						new StockItemRepository(session).delete(item);
						log.info("Picking: stock item {} deleted, nothing was found", stockItemId);
						sendToteIfEmpty(session, tote);
					}

					order.setStatus(RetrievalOrderStatus.NEW);
					log.info("Picking: order {} is NEW again, the missing part will be reserved", order.getOrderId());
				}
			}.run();
		} catch (RuntimeException e) {
			if (e.getCause() instanceof ResponseStatusException) {
				throw (ResponseStatusException) e.getCause();
			}
			throw e;
		}
	}

	// aceeasi inregistrare ca la inventar: o verificare pe tote si diferenta gasita
	// motivul vine de la ecran: DAMAGED sau MISSING
	private void saveStockAdjustment(Session session, StockItem item, int found, String reason) {
		LocalDateTime now = LocalDateTime.now();
		InventoryOrder inventoryOrder = new InventoryOrder();
		inventoryOrder.setCreatedAt(now);
		inventoryOrder.setReasonMessage(reason);
		inventoryOrder.setCode("PICKING-" + item.getId() + "-" + System.currentTimeMillis());
		inventoryOrder.setStatus(InventoryOrderStatus.FINISHED);
		inventoryOrder.setFinishedAt(now);
		inventoryOrder = new InventoryOrderRepository(session).save(inventoryOrder);

		InventoryOrderLine inventoryOrderLine = new InventoryOrderLine();
		inventoryOrderLine.setInventoryOrder(inventoryOrder);
		inventoryOrderLine.setLoadUnit(item.getLoadUnit());
		inventoryOrderLine.setLoadUnitBarcode(item.getLoadUnit().getBarcode());
		inventoryOrderLine.setLoadUnitType(item.getLoadUnit().getType().toString());
		inventoryOrderLine.setLocationName(
				item.getLoadUnit().getLocation() == null ? null : item.getLoadUnit().getLocation().getName());
		inventoryOrderLine.setStatus(InventoryOrderLineStatus.FINISHED);
		inventoryOrderLine.setCompletedAt(now);
		inventoryOrderLine = new InventoryOrderLineRepository(session).save(inventoryOrderLine);

		ArticlePackagingUnitType packaging = item.getArticlePackagingUnitType();
		StockAdjustment adjustment = new StockAdjustment();
		adjustment.setInventoryOrderLine(inventoryOrderLine);
		adjustment.setArticle(packaging.getArticle());
		adjustment.setArticleBarcode(packaging.getArticle().getBarcode());
		adjustment.setArticleDescription(packaging.getArticle().getDescription());
		adjustment.setArticlePackagingUnitType(packaging);
		adjustment.setPackagingUnitType(packaging.getPackagingUnitType());
		adjustment.setExpectedQuantity(item.getQuantity());
		adjustment.setCountedQuantity(found);
		adjustment.setDifferenceQuantity(found - item.getQuantity());
		adjustment.setReasonMessage(StockAdjustmentReason.valueOf(reason));
		new StockAdjustmentRepository(session).save(adjustment);
	}

	// daca tote-ul din care am luat a ramas gol, il trimitem la EMPTY_TOTES ca sa
	// se elibereze locatia de picking
	// daca nu exista locatia EMPTY_TOTES, tote-ul ramane pe locatie si pickingul
	// merge mai departe
	private void sendToteIfEmpty(Session session, LoadUnit tote) {
		if (tote.getType() == LoadUnitType.TOTE
				&& new StockItemRepository(session).findByLoadUnitOrderByIdAsc(tote).isEmpty()) {
			Location emptyTotes = new LocationRepository(session).findByName(Constants.EMPTY_TOTES).orElse(null);
			if (emptyTotes == null) {
				log.warn("Picking: tote {} is empty, but there is no {} location, the tote stays", tote.getBarcode(),
						Constants.EMPTY_TOTES);
			} else {
				transportOrderService.createTransportOrder(session, tote, emptyTotes);
				log.info("Picking: tote {} is empty, transport order to {}", tote.getBarcode(), Constants.EMPTY_TOTES);
			}
		}
	}

	// linia e gata cand bucatile din cutie ajung la cantitatea comandata
	// (dupa o eroare pot fi mai putine, atunci linia ramane deschisa)
	private boolean isLineInBox(RetrievalOrderLine line) {
		int pieces = 0;
		for (Reservation reservation : line.getReservations()) {
			StockItem item = reservation.getStockItem();
			if (item.getLoadUnit().getType() == LoadUnitType.CARTON) {
				pieces = pieces + item.getTotalQuantity();
			}
		}
		return pieces >= line.getQuantity();
	}

	// muncitorul a scanat iar cutia: daca toate liniile sunt gata, cutia merge la
	// OUT si comanda devine PICKED
	public String closeBox(String orderId, String boxBarcode) {
		try {
			return new WithSessionAndTransaction<String>() {
				@Override
				protected void executeBusinessLogic(Session session) {
					long open = new RetrievalOrderLineRepository(session).countByOrderOrderIdAndStatusNot(orderId,
							RetrievalOrderLineStatus.PICKED);
					if (open > 0) {
						throw new ResponseStatusException(HttpStatus.BAD_REQUEST, open + " lines are not finished yet");
					}

					Location out = new LocationRepository(session).findByName(Constants.OUT)
							.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
									"Cannot find location " + Constants.OUT));
					transportOrderService.createTransportOrder(session, findBox(session, boxBarcode), out);

					findOrder(session, orderId).setStatus(RetrievalOrderStatus.PICKED);
					log.info("Picking: box {} closed, transport order to {}", boxBarcode, Constants.OUT);
					setReturnValue("Box " + boxBarcode + " goes to " + Constants.OUT);
				}
			}.run();
		} catch (RuntimeException e) {
			if (e.getCause() instanceof ResponseStatusException) {
				throw (ResponseStatusException) e.getCause();
			}
			throw e;
		}
	}

	// toate liniile au destule bucati rezervate in locatii PICK (getTotalQuantity =
	// baxuri x bucati pe bax)
	private boolean isInPicking(RetrievalOrder order) {
		for (RetrievalOrderLine line : order.getLines()) {
			int pieces = 0;
			for (Reservation reservation : line.getReservations()) {
				StockItem item = reservation.getStockItem();
				if (item.getLoadUnit().getLocation().getName().startsWith(Constants.PICK_PREFIX)) {
					pieces = pieces + item.getTotalQuantity();
				}
			}
			if (pieces < line.getQuantity()) {
				return false;
			}
		}
		return true;
	}

	// ce primeste frontendul: comanda, cutia (daca exista deja) si liniile cu
	// bucatile deja mutate in cutie
	// si cu pasii ramasi, cate un pas de stock item rezervat
	private PickingOrderResponse toResponse(RetrievalOrder order) {
		String box = null;
		List<PickingLineResponse> lines = new ArrayList<>();
		for (RetrievalOrderLine line : order.getLines()) {
			int moved = 0;
			List<PickingStepResponse> steps = new ArrayList<>();
			for (Reservation reservation : line.getReservations()) {
				StockItem item = reservation.getStockItem();
				LoadUnit loadUnit = item.getLoadUnit();
				if (loadUnit.getType() == LoadUnitType.CARTON) {
					box = loadUnit.getBarcode();
					moved = moved + item.getTotalQuantity();
				} else {
					steps.add(new PickingStepResponse(item.getId(), loadUnit.getLocation().getName(),
							item.getArticlePackagingUnitType().getBarcode(), item.getQuantity(),
							item.getArticlePackagingUnitType().getPackagingUnitType().getQuantity()));
				}
			}
			lines.add(new PickingLineResponse(line.getArticle().getBarcode(), line.getArticle().getDescription(),
					line.getQuantity(), moved, line.getStatus().toString(), steps));
		}
		return new PickingOrderResponse(order.getOrderId(), order.getStatus().toString(),
				order.getDestination().getName(), box, lines);
	}

	private StockItem findStockItem(Session session, Long stockItemId) {
		return new StockItemRepository(session).findById(stockItemId).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot find stock item " + stockItemId));
	}

	private RetrievalOrder findOrder(Session session, String orderId) {
		return new RetrievalOrderRepository(session).findByOrderId(orderId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot find order " + orderId));
	}

	private LoadUnit findBox(Session session, String barcode) {
		return new LoadUnitRepository(session).findByBarcode(barcode)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot find box " + barcode));
	}
}