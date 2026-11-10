package ro.demo.usersbackend.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ro.demo.usersbackend.dto.PickingLineResponse;
import ro.demo.usersbackend.dto.PickingOrderResponse;
import ro.demo.usersbackend.dto.PickingStepResponse;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.LoadUnitType;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.Reservation;
import ro.demo.usersbackend.entity.RetrievalOrder;
import ro.demo.usersbackend.entity.RetrievalOrderLine;
import ro.demo.usersbackend.entity.RetrievalOrderLineStatus;
import ro.demo.usersbackend.entity.RetrievalOrderStatus;
import ro.demo.usersbackend.entity.StockItem;
import ro.demo.usersbackend.repository.LoadUnitRepository;
import ro.demo.usersbackend.repository.LocationRepository;
import ro.demo.usersbackend.repository.RetrievalOrderLineRepository;
import ro.demo.usersbackend.repository.RetrievalOrderRepository;
import ro.demo.usersbackend.repository.StockItemRepository;

// toata logica statiei de picking, pe entitati
@Service
@Transactional
public class PickingService {
	public static final Logger log = LogManager.getLogger("application");

	private final RetrievalOrderRepository retrievalOrderRepository;
	private final RetrievalOrderLineRepository retrievalOrderLineRepository;
	private final LoadUnitRepository loadUnitRepository;
	private final LocationRepository locationRepository;
	private final TransportOrderService transportOrderService;
	private final StockItemRepository stockItemRepository;

	public PickingService(RetrievalOrderRepository retrievalOrderRepository,
			RetrievalOrderLineRepository retrievalOrderLineRepository, LoadUnitRepository loadUnitRepository,
			LocationRepository locationRepository, TransportOrderService transportOrderService,
			StockItemRepository stockItemRepository) {
		this.retrievalOrderRepository = retrievalOrderRepository;
		this.retrievalOrderLineRepository = retrievalOrderLineRepository;
		this.loadUnitRepository = loadUnitRepository;
		this.locationRepository = locationRepository;
		this.transportOrderService = transportOrderService;
		this.stockItemRepository = stockItemRepository;
	}

	// intai comenzile incepute (STARTED, cutia e deja scanata), apoi cele rezervate (ACTIVE), cea mai veche prima
	// se ia prima care are toata marfa in zona de picking, sau null daca nu e niciuna
	@Transactional(readOnly = true)
	public PickingOrderResponse findNextOrder() {
		List<RetrievalOrder> orders = new ArrayList<>(retrievalOrderRepository.findByStatusOrderByIdAsc(RetrievalOrderStatus.STARTED));
		orders.addAll(retrievalOrderRepository.findByStatusOrderByIdAsc(RetrievalOrderStatus.ACTIVE));
		for (RetrievalOrder order : orders) {
			if (isInPicking(order)) {
				return toResponse(order);
			}
		}
		return null;
	}

	@Transactional(readOnly = true)
	public PickingOrderResponse findOrderDetails(String orderId) {
		return toResponse(findOrder(orderId));
	}

	// muncitorul a scanat cutia: o cream ca load unit CARTON in locatia PICK unde e marfa comenzii
	public String createBox(String orderId, String barcode) {
		if (loadUnitRepository.findByBarcode(barcode).isPresent()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Box " + barcode + " already exists");
		}
		RetrievalOrder order = findOrder(orderId);

		LoadUnit box = new LoadUnit();
		box.setBarcode(barcode);
		box.setType(LoadUnitType.CARTON);
		box.setLocation(order.getLines().get(0).getReservations().get(0).getStockItem().getLoadUnit().getLocation());
		loadUnitRepository.save(box);

		order.setStatus(RetrievalOrderStatus.STARTED);
		log.info("Picking: box {} created for order {}", barcode, orderId);
		return "Box " + barcode + " ready";
	}

	// muncitorul a apasat confirm: stock itemul din pasul de pe ecran intra in cutie
	// il luam dupa id, pentru ca acelasi barcode poate fi in mai multe locatii
	// linia e gata doar cand toate stock itemurile ei sunt in cutie
	// muncitorul a apasat confirm: stock itemul din pasul de pe ecran intra in cutie
	// il luam dupa id, pentru ca acelasi barcode poate fi in mai multe locatii
	// linia e gata doar cand toate stock itemurile ei sunt in cutie
	public void moveItemToBox(Long stockItemId, String boxBarcode) {
		LoadUnit box = findBox(boxBarcode);
		StockItem item = stockItemRepository.findById(stockItemId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot find stock item " + stockItemId));
		item.setLoadUnit(box);

		RetrievalOrderLine line = item.getReservation().getRetrievalOrderLine();
		if (isLineInBox(line)) {
			line.setStatus(RetrievalOrderLineStatus.PICKED);
		}
		log.info("Picking: stock item {} ({}) moved into box {}", stockItemId, line.getArticle().getDescription(), boxBarcode);
	}

	private boolean isLineInBox(RetrievalOrderLine line) {
		for (Reservation reservation : line.getReservations()) {
			if (reservation.getStockItem().getLoadUnit().getType() != LoadUnitType.CARTON) {
				return false;
			}
		}
		return true;
	}

	// muncitorul a scanat iar cutia: daca toate liniile sunt gata, cutia merge la OUT si comanda devine PICKED
	public String closeBox(String orderId, String boxBarcode) {
		long open = retrievalOrderLineRepository.countByOrderOrderIdAndStatusNot(orderId, RetrievalOrderLineStatus.PICKED);
		if (open > 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, open + " lines are not finished yet");
		}

		Location out = locationRepository.findByName("OUT")
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot find the OUT location"));
		transportOrderService.createTransportOrder(findBox(boxBarcode), out);

		findOrder(orderId).setStatus(RetrievalOrderStatus.PICKED);
		log.info("Picking: box {} closed, transport order to OUT", boxBarcode);
		return "Box " + boxBarcode + " goes to OUT";
	}

	// toate liniile au destule bucati rezervate in locatii PICK (getTotalQuantity = baxuri x bucati pe bax)
	private boolean isInPicking(RetrievalOrder order) {
		for (RetrievalOrderLine line : order.getLines()) {
			int pieces = 0;
			for (Reservation reservation : line.getReservations()) {
				StockItem item = reservation.getStockItem();
				if (item.getLoadUnit().getLocation().getName().startsWith("PICK")) {
					pieces = pieces + item.getTotalQuantity();
				}
			}
			if (pieces < line.getQuantity()) {
				return false;
			}
		}
		return true;
	}

	// ce primeste frontendul: comanda, cutia (daca exista deja) si liniile cu bucatile deja mutate in cutie
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
		return new PickingOrderResponse(order.getOrderId(), order.getDestination().getName(), box, lines);
	}

	private RetrievalOrder findOrder(String orderId) {
		return retrievalOrderRepository.findByOrderId(orderId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot find order " + orderId));
	}

	private LoadUnit findBox(String barcode) {
		return loadUnitRepository.findByBarcode(barcode)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot find box " + barcode));
	}
}
