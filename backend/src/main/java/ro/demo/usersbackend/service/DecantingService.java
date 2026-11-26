package ro.demo.usersbackend.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.dto.DecantingItem;
import ro.demo.usersbackend.dto.DecantingResponse;
import ro.demo.usersbackend.dto.DecantingTransferRequest;
import ro.demo.usersbackend.entity.ArticlePackagingUnitType;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.LoadUnitType;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.Reservation;
import ro.demo.usersbackend.entity.RetrievalOrderLine;
import ro.demo.usersbackend.entity.StockItem;
import ro.demo.usersbackend.repository.LoadUnitRepository;
import ro.demo.usersbackend.repository.LocationRepository;
import ro.demo.usersbackend.repository.ReservationRepository;
import ro.demo.usersbackend.repository.StockItemRepository;

@Service
public class DecantingService {

	public DecantingService() {
		super();
	}

	public String createEmptyTote() {
		return new WithSessionAndTransaction<String>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				setReturnValue(createTote(session).getBarcode());
			}
		}.run();
	}

	public DecantingResponse start(String loadUnitBarcode) {
		return new WithSessionAndTransaction<DecantingResponse>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				LoadUnit loadUnit = findLoadUnit(session, loadUnitBarcode);
				if (loadUnit.getType() == LoadUnitType.TOTE) {
					throw badRequest("Load unit " + loadUnitBarcode + " is a tote. Scan a pallet or a carton.");
				}
				checkAtDecanting(loadUnit);

				List<StockItem> items = availableItems(session, loadUnit);
				if (items.isEmpty()) {
					throw badRequest("Load unit " + loadUnitBarcode + " is empty.");
				}

				setReturnValue(response(loadUnit, null, items, false));
			}
		}.run();
	}

	public DecantingResponse transfer(DecantingTransferRequest request) {
		return new WithSessionAndTransaction<DecantingResponse>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				LoadUnit loadUnit = findLoadUnit(session, request.loadUnitBarcode());
				checkAtDecanting(loadUnit);
				List<StockItem> matching = availableItems(session, loadUnit).stream()
						.filter(s -> s.getArticlePackagingUnitType().getBarcode().equals(request.packagingBarcode()))
						.sorted(Comparator.comparing((StockItem s) -> s.getReservation() == null)).toList();
				if (matching.isEmpty()) {
					throw badRequest("Item " + request.packagingBarcode() + " is not on load unit "
							+ request.loadUnitBarcode() + ".");
				}

				int total = matching.stream().mapToInt(StockItem::getQuantity).sum();
				int quantity = request.quantity();
				if (quantity <= 0) {
					throw badRequest("Quantity must be greater than 0.");
				}
				if (quantity > total) {
					throw badRequest("Cannot move " + quantity + ": only " + total + " available.");
				}

				LoadUnit tote = createTote(session);

				int left = quantity;
				for (StockItem item : matching) {
					if (left == 0) {
						break;
					}
					if (left >= item.getQuantity()) {
						left -= item.getQuantity();
						item.setLoadUnit(tote);
					} else {
						item.setQuantity(item.getQuantity() - left);
						StockItem splitItem = new StockItemRepository(session)
								.save(new StockItem(item.getArticlePackagingUnitType(), left, tote));
						if (item.getReservation() != null) {
							reserveForSameLine(session, splitItem, item.getReservation().getRetrievalOrderLine());
						}
						left = 0;
					}
				}

				List<StockItem> remaining = availableItems(session, loadUnit);
				setReturnValue(response(loadUnit, tote, remaining, remaining.isEmpty()));
			}
		}.run();
	}

	private List<StockItem> availableItems(Session session, LoadUnit loadUnit) {
		return new StockItemRepository(session).findByLoadUnitOrderByIdAsc(loadUnit);
	}

	private void reserveForSameLine(Session session, StockItem splitItem, RetrievalOrderLine line) {
		Reservation reservation = new ReservationRepository(session).save(new Reservation(splitItem, line));
		splitItem.setReservation(reservation);
	}

	private LoadUnit createTote(Session session) {
		Location decanting = new LocationRepository(session).findByName(Constants.DECANTING)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
						"Location " + Constants.DECANTING + " is missing."));

		LoadUnit tote = new LoadUnit();
		tote.setBarcode(generateToteBarcode(session));
		tote.setType(LoadUnitType.TOTE);
		tote.setLocation(decanting);
		return new LoadUnitRepository(session).save(tote);
	}

	// sarim peste barcode-urile folosite deja de alt load unit (create din SQL, Sim Management sau dialog)
	private String generateToteBarcode(Session session) {
		LoadUnitRepository loadUnitRepository = new LoadUnitRepository(session);
		String barcode;
		do {
			Long next = session
					.createNativeQuery("SELECT " + Constants.TOTE_SEQUENCE + ".NEXTVAL FROM DUAL", Long.class)
					.getSingleResult();
			barcode = String.format(Constants.TOTE_BARCODE, next);
		} while (loadUnitRepository.findByBarcode(barcode).isPresent());
		return barcode;
	}

	private LoadUnit findLoadUnit(Session session, String barcode) {
		return new LoadUnitRepository(session).findByBarcode(barcode)
				.orElseThrow(() -> badRequest("Load unit " + barcode + " not found."));
	}

	private void checkAtDecanting(LoadUnit loadUnit) {
		if (loadUnit.getLocation() == null || !Constants.DECANTING.equals(loadUnit.getLocation().getName())) {
			throw badRequest("Load unit " + loadUnit.getBarcode() + " is not at " + Constants.DECANTING
					+ ". Finish its transport first.");
		}
	}

//	private DecantingResponse response(LoadUnit loadUnit, LoadUnit tote, List<StockItem> items, boolean finished) {
//		return new DecantingResponse(loadUnit.getBarcode(), tote == null ? null : tote.getBarcode(),
//				items.stream().map(this::toItem).toList(), finished);
//	}

	private DecantingResponse response(LoadUnit loadUnit, LoadUnit tote, List<StockItem> items, boolean finished) {
		return new DecantingResponse(loadUnit.getBarcode(), tote == null ? null : tote.getBarcode(), toItems(items),
				finished);
	}

//	private DecantingItem toItem(StockItem stockItem) {
//		ArticlePackagingUnitType packaging = stockItem.getArticlePackagingUnitType();
//		return new DecantingItem(packaging.getBarcode(), packaging.getArticle().getBarcode(),
//				packaging.getArticle().getDescription(), packaging.getPackagingUnitType().getName(),
//				stockItem.getQuantity());
//	}

	private List<DecantingItem> toItems(List<StockItem> stockItems) {
		Map<String, DecantingItem> byBarcode = new LinkedHashMap<>();
		for (StockItem s : stockItems) {
			ArticlePackagingUnitType p = s.getArticlePackagingUnitType();
			int reserved = s.getReservation() != null ? s.getQuantity() : 0;
			DecantingItem line = new DecantingItem(p.getBarcode(), p.getArticle().getBarcode(),
					p.getArticle().getDescription(), p.getPackagingUnitType().getName(), s.getQuantity(), reserved);

			byBarcode.merge(p.getBarcode(), line,
					(a, b) -> new DecantingItem(a.packagingBarcode(), a.articleBarcode(), a.description(),
							a.packaging(), a.quantity() + b.quantity(), a.reservedQuantity() + b.reservedQuantity()));
		}
		return List.copyOf(byBarcode.values());
	}

	private ResponseStatusException badRequest(String message) {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
	}
}