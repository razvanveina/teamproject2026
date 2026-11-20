package ro.demo.usersbackend.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.transaction.Transactional;
import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.dto.TransportOrderResponse;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.StorageLocation;
import ro.demo.usersbackend.entity.TransportOrder;
import ro.demo.usersbackend.entity.TransportOrderErrorReason;
import ro.demo.usersbackend.entity.TransportOrderStatus;
import ro.demo.usersbackend.repository.StorageLocationRepository;
import ro.demo.usersbackend.repository.TransportOrderRepository;
import ro.demo.usersbackend.server.mfs.StationControl;

@Service
public class TransportOrderService {

	private Location decanting() {
		return new WithSessionAndTransaction<Location>() {

			@Override
			protected void executeBusinessLogic(Session session) {
				StorageLocationRepository locationRepository = new StorageLocationRepository(session);
				Optional<StorageLocation> location = locationRepository.findByName(Constants.DECANTING);

				if (location.isPresent()) {
					setReturnValue(location.get());
				} else {
					throw new ResponseStatusException(HttpStatus.NOT_FOUND,
							"Cannot find location with the name " + Constants.DECANTING);
				}

			}
		}.run();
	}

	private boolean isTote(TransportOrder order) {
		String source = order.getSource().getName();
		return Constants.DECANTING.equals(source) || Constants.TPT1.equals(source);
	}

	// TO-urile spre un punct de pe banda le face simulatorul, nu un muncitor, adica
	// nu apar pe ecranele de transport
	private boolean isAutomatic(TransportOrder order) {
		return Constants.CONVEYOR_POINTS.contains(order.getTarget().getName());
	}

	private TransportOrder find(Long id) {
		return new WithSessionAndTransaction<TransportOrder>() {

			@Override
			protected void executeBusinessLogic(Session session) {
				TransportOrderRepository transportOrderRepository = new TransportOrderRepository(session);
				Optional<TransportOrder> to = transportOrderRepository.findById(id);

				if (to.isPresent()) {
					setReturnValue(to.get());
				} else {
					throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transport order not found");
				}
			}
		}.run();
	}

	private Comparator<TransportOrder> activeFirst() {
		return Comparator.comparing((TransportOrder o) -> o.getStatus() != TransportOrderStatus.ACTIVE);
	}

	public Optional<TransportOrderResponse> findNextOrder(String user) {
		return nextFor(user, false);
	}

	public Optional<TransportOrderResponse> findNextToteOrder(String user) {
		return nextFor(user, true);
	}

	private Optional<TransportOrderResponse> nextFor(String user, boolean tote) {
		return new WithSessionAndTransaction<Optional<TransportOrderResponse>>() {

			@Override
			protected void executeBusinessLogic(Session session) {
				TransportOrderRepository transportOrderRepository = new TransportOrderRepository(session);
				Optional<TransportOrder> mine = transportOrderRepository
						.findByStatusAndAssignedToOrderByIdAsc(user, Constants.OPEN_TRANSPORT_STATUSES).stream()
						.filter(o -> !isAutomatic(o)).filter(o -> isTote(o) == tote).sorted(activeFirst()).findFirst();
				if (mine.isPresent()) {
					setReturnValue(mine.map(this::toResponse));
				}

				Optional<Object> to = transportOrderRepository
						.findByStatusAndAssignedToIsNullOrderByIdAsc(Constants.OPEN_TRANSPORT_STATUSES).stream()
						.filter(o -> !isAutomatic(o)).filter(o -> isTote(o) == tote).sorted(activeFirst()).findFirst()
						.map(order -> {
							order.setAssignedTo(user);
							return toResponse(order);
						});
				setReturnValue(to);
			}
		}.run();
	}

	@Transactional(readOnly = true)
	public List<TransportOrderResponse> findOpenOrders(String user) {
		return transportOrderRepository.findByStatusAndNotSourceOrderByIdAsc(TransportOrderStatus.NEW, decanting())
				.stream().filter(o -> !isAutomatic(o))
				.filter(o -> o.getAssignedTo() == null || user.equals(o.getAssignedTo())).map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<TransportOrderResponse> findOpenToteOrders(String user) {
		return transportOrderRepository.findByStatusAndSourceOrderedByIdAsc(TransportOrderStatus.NEW, decanting())
				.stream().filter(o -> !isAutomatic(o))
				.filter(o -> o.getAssignedTo() == null || user.equals(o.getAssignedTo())).map(this::toResponse)
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
		for (TransportOrder other : transportOrderRepository.findByStatusAndAssignedToOrderByIdAsc(user,
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
			order.setAssignedTo(user); // transport vechi, fara user: il preia acum
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
			if (Constants.TPT1.equals(order.getSource().getName())) {
				releaseTPT1();
			}
		}
		return toResponse(order);
	}

	private void releaseTPT1() {
		try {
			StationControl.getInstance().release(Constants.TPT1);
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
					"cannot release " + Constants.TPT1 + ": " + e.getMessage());
		}
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
		if (target instanceof StorageLocation && target.getLoadUnit() != null
				&& !target.getLoadUnit().equals(loadUnit)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Location " + target.getName() + " is already occupied");
		}
		loadUnit.setLocation(target); // this alone frees the old location and fills the new one
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