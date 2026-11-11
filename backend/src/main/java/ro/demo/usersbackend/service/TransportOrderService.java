package ro.demo.usersbackend.service;

import java.util.List;
import java.util.Optional;
import java.util.Comparator;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ro.demo.usersbackend.dto.TransportOrderResponse;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.StorageLocation;
import ro.demo.usersbackend.entity.TransportOrder;
import ro.demo.usersbackend.entity.TransportOrderErrorReason;
import ro.demo.usersbackend.entity.TransportOrderStatus;
import ro.demo.usersbackend.repository.LocationRepository;
import ro.demo.usersbackend.repository.StorageLocationRepository;
import ro.demo.usersbackend.repository.TransportOrderRepository;

@Service
@Transactional
public class TransportOrderService {

	private static final String DECANTING = "DECANTING";

	private final TransportOrderRepository transportOrderRepository;
	private final LocationRepository locationRepository;
	private final StorageLocationRepository storageLocationRepository;

	public TransportOrderService(TransportOrderRepository transportOrderRepository,
			LocationRepository locationRepository, StorageLocationRepository storageLocationRepository) {
		super();
		this.transportOrderRepository = transportOrderRepository;
		this.locationRepository = locationRepository;
		this.storageLocationRepository = storageLocationRepository;
	}

	private Location decanting() {
		return locationRepository.findByName(DECANTING)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Cannot find location with the name " + DECANTING));
	}
	
	private static final List<TransportOrderStatus> OPEN = List.of(TransportOrderStatus.ACTIVE, TransportOrderStatus.NEW);

	private static boolean isTote(TransportOrder order) {
		return DECANTING.equals(order.getSource().getName());
	}

	private TransportOrder find(Long id) {
		return transportOrderRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transport order not found"));
	}

	private static Comparator<TransportOrder> activeFirst() {
		return Comparator.comparing((TransportOrder o) -> o.getStatus() != TransportOrderStatus.ACTIVE);
	}

	public Optional<TransportOrderResponse> findNextOrder(String user) {
		return nextFor(user, false);
	}

	public Optional<TransportOrderResponse> findNextToteOrder(String user) {
		return nextFor(user, true);
	}

	private Optional<TransportOrderResponse> nextFor(String user, boolean tote) {
		// 1. are deja un transport preluat? il continua
		Optional<TransportOrder> mine = transportOrderRepository.findByAssignedToAndStatusInOrderByIdAsc(user, OPEN)
				.stream().filter(o -> isTote(o) == tote).sorted(activeFirst()).findFirst();
		if (mine.isPresent()) {
			return mine.map(this::toResponse);
		}

		// 2. altfel il preia pe cel mai vechi liber
		return transportOrderRepository.findByAssignedToIsNullAndStatusInOrderByIdAsc(OPEN)
				.stream().filter(o -> isTote(o) == tote).sorted(activeFirst()).findFirst()
				.map(order -> {
					order.setAssignedTo(user);
					return toResponse(order);
				});
	}

	@Transactional(readOnly = true)
	public List<TransportOrderResponse> findOpenOrders(String user) {
		return transportOrderRepository.findByStatusAndSourceNotOrderByIdAsc(TransportOrderStatus.NEW, decanting())
				.stream()
				.filter(o -> o.getAssignedTo() == null || user.equals(o.getAssignedTo()))
				.map(this::toResponse)
				.toList();
	}
	
	@Transactional(readOnly = true)
	public List<TransportOrderResponse> findOpenToteOrders(String user) {
		return transportOrderRepository.findByStatusAndSourceOrderByIdAsc(TransportOrderStatus.NEW, decanting())
				.stream()
				.filter(o -> o.getAssignedTo() == null || user.equals(o.getAssignedTo()))
				.map(this::toResponse)
				.toList();
	}

	public TransportOrderResponse take(Long id, String user) {
		TransportOrder order = find(id);
		if (!user.equals(order.getAssignedTo())) {
			if (order.getAssignedTo() != null || order.getStatus() != TransportOrderStatus.NEW) {
				throw new ResponseStatusException(HttpStatus.CONFLICT, "Transport order " + id + " is already taken"
						+ (order.getAssignedTo() == null ? "" : " by " + order.getAssignedTo()));
			}
			order.setAssignedTo(user);
		}

		// eliberez celelalte transporturi NEW ale mele de acelasi tip
		for (TransportOrder other : transportOrderRepository.findByAssignedToAndStatusInOrderByIdAsc(user,
				List.of(TransportOrderStatus.NEW))) {
			if (!other.getId().equals(id) && isTote(other) == isTote(order)) {
				other.setAssignedTo(null);
			}
		}
		return toResponse(order);
	}

	public void release(Long id, String user) {
		TransportOrder order = find(id);
		if (user.equals(order.getAssignedTo()) && order.getStatus() == TransportOrderStatus.NEW) {
			order.setAssignedTo(null);
		}
	}

	private TransportOrder ownedOrder(Long id, String user) {
		TransportOrder order = find(id);
		if (order.getAssignedTo() == null) {
			order.setAssignedTo(user);   // transport vechi, fara user: il preia acum
		}
		if (!user.equals(order.getAssignedTo())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Transport order " + id + " is taken by " + order.getAssignedTo());
		}
		return order;
	}

	public TransportOrderResponse scan(Long id, String barcode, String user) {
		TransportOrder order = ownedOrder(id, user);
		String scanned = barcode.trim();

		if (order.getStatus() == TransportOrderStatus.NEW) {
			if (!order.getLoadUnit().getBarcode().equalsIgnoreCase(scanned)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Wrong pallet. Scan pallet " + order.getLoadUnit().getBarcode());
			}
			order.setStatus(TransportOrderStatus.ACTIVE);
		} else if (order.getStatus() == TransportOrderStatus.ACTIVE) {
			if (!order.getTarget().getName().equalsIgnoreCase(scanned)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Wrong location. Scan location " + order.getTarget().getName());
			}
			moveLoadUnit(order.getLoadUnit(), order.getTarget());
			order.setStatus(TransportOrderStatus.FINISHED);
		}
		return toResponse(order);
	}

	public TransportOrderResponse reportError(Long id, TransportOrderErrorReason reason, String user) {
		TransportOrder order = ownedOrder(id, user);

		boolean allowed = switch (reason) {
		case LOCATION_EMPTY, WRONG_PALLET -> order.getStatus() == TransportOrderStatus.NEW;
		case TARGET_LOCATION_OCCUPIED -> order.getStatus() == TransportOrderStatus.ACTIVE;
		};
		if (!allowed) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Error " + reason + " cannot be reported for an order with status " + order.getStatus());
		}

		order.setErrorReason(reason);
		order.setStatus(TransportOrderStatus.ERROR);
		return toResponse(order);
	}

	private void moveLoadUnit(LoadUnit loadUnit, Location target) {
		if (target instanceof StorageLocation storage && storage.getLoadUnit() != null
				&& !storage.getLoadUnit().equals(loadUnit)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Location " + target.getName() + " is already occupied");
		}

		for (StorageLocation previous : storageLocationRepository.findByLoadUnit(loadUnit)) {
			if (!previous.equals(target)) {
				previous.setLoadUnit(null);
			}
		}
		transportOrderRepository.flush();
		if (target instanceof StorageLocation storage) {
			storage.setLoadUnit(loadUnit);
		}
		loadUnit.setLocation(target);
	}

	private TransportOrderResponse toResponse(TransportOrder order) {
		String loadUnitLocation = order.getLoadUnit().getLocation() == null ? null
				: order.getLoadUnit().getLocation().getName();
		return new TransportOrderResponse(order.getId(), order.getStatus(), order.getLoadUnit().getBarcode(),
				order.getSource().getName(), order.getTarget().getName(), loadUnitLocation);
	}

	public TransportOrderResponse createTransportOrder(LoadUnit loadUnit, Location target) {
		TransportOrder order = new TransportOrder(loadUnit.getLocation(), target, TransportOrderStatus.NEW, loadUnit);

		transportOrderRepository.save(order);

		return toResponse(order);
	}
}
