package ro.demo.usersbackend.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.dto.TransportOrderResponse;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.LoadUnitType;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.StorageLocation;
import ro.demo.usersbackend.entity.TransportOrder;
import ro.demo.usersbackend.entity.TransportOrderErrorReason;
import ro.demo.usersbackend.entity.TransportOrderStatus;
import ro.demo.usersbackend.repository.LocationRepository;
import ro.demo.usersbackend.repository.TransportOrderRepository;
import ro.demo.usersbackend.server.mfs.StationControl;

@Service
public class TransportOrderService {

	public TransportOrderService() {
		super();
	}

	private Location decanting(Session session) {
		return new LocationRepository(session).findByName(Constants.DECANTING)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Cannot find location with the name " + Constants.DECANTING));
	}

	private boolean isTote(TransportOrder order) {
		String source = order.getSource().getName();
		return Constants.DECANTING.equals(source) || Constants.TPT1.equals(source);
	}

	// TO-urile spre un punct de pe banda, tote-urile de la decanting si cele care
	// pleaca din PTTT spre OUT le face simulatorul, nu un muncitor
	private boolean isAutomatic(TransportOrder order) {
		return Constants.CONVEYOR_POINTS.contains(order.getTarget().getName())
				|| Constants.PTTT_POINTS.contains(order.getSource().getName())
				|| (Constants.DECANTING.equals(order.getSource().getName())
						&& order.getLoadUnit().getType() == LoadUnitType.TOTE);
	}

	private TransportOrder find(Session session, Long id) {
		return new TransportOrderRepository(session).findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transport order not found"));
	}

	private Comparator<TransportOrder> activeFirst() {
		return Comparator.comparing((TransportOrder o) -> o.getStatus() != TransportOrderStatus.ACTIVE);
	}

	public Optional<TransportOrderResponse> findNextOrder(String user) {
		return new WithSessionAndTransaction<Optional<TransportOrderResponse>>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				setReturnValue(nextFor(session, user, false));
			}
		}.run();
	}

	public Optional<TransportOrderResponse> findNextToteOrder(String user) {
		return new WithSessionAndTransaction<Optional<TransportOrderResponse>>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				setReturnValue(nextFor(session, user, true));
			}
		}.run();
	}

	private Optional<TransportOrderResponse> nextFor(Session session, String user, boolean tote) {
		TransportOrderRepository transportOrderRepository = new TransportOrderRepository(session);

		// 1. are deja un transport preluat? il continua
		Optional<TransportOrder> mine = transportOrderRepository
				.findByStatusAndAssignedToOrderByIdAsc(user, Constants.OPEN_TRANSPORT_STATUSES).stream()
				.filter(o -> !isAutomatic(o)).filter(o -> isTote(o) == tote).sorted(activeFirst()).findFirst();
		if (mine.isPresent()) {
			return mine.map(this::toResponse);
		}

		// 2. altfel il preia pe cel mai vechi liber
		return transportOrderRepository.findByStatusAndAssignedToIsNullOrderByIdAsc(Constants.OPEN_TRANSPORT_STATUSES)
				.stream().filter(o -> !isAutomatic(o)).filter(o -> isTote(o) == tote).sorted(activeFirst()).findFirst()
				.map(order -> {
					order.setAssignedTo(user);
					return toResponse(order);
				});
	}

	public List<TransportOrderResponse> findOpenOrders(String user) {
		return new WithSessionAndTransaction<List<TransportOrderResponse>>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				List<TransportOrder> openOrders = new TransportOrderRepository(session)
						.findByStatusAndNotSourceOrderByIdAsc(TransportOrderStatus.NEW, decanting(session));
				setReturnValue(visibleTo(user, openOrders));
			}
		}.run();
	}

	public List<TransportOrderResponse> findOpenToteOrders(String user) {
		return new WithSessionAndTransaction<List<TransportOrderResponse>>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				List<TransportOrder> openToteOrders = new TransportOrderRepository(session)
						.findByStatusAndSourceOrderedByIdAsc(TransportOrderStatus.NEW, decanting(session));
				setReturnValue(visibleTo(user, openToteOrders));
			}
		}.run();
	}

	private List<TransportOrderResponse> visibleTo(String user, List<TransportOrder> orders) {
		return orders.stream().filter(o -> !isAutomatic(o))
				.filter(o -> o.getAssignedTo() == null || user.equals(o.getAssignedTo())).map(this::toResponse)
				.toList();
	}

	public TransportOrderResponse take(Long id, String user) {
		return new WithSessionAndTransaction<TransportOrderResponse>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				TransportOrder order = find(session, id);
				if (!user.equals(order.getAssignedTo())) {
					if (order.getAssignedTo() != null || order.getStatus() != TransportOrderStatus.NEW) {
						throw new ResponseStatusException(HttpStatus.CONFLICT,
								"Transport order " + id + " is already taken"
										+ (order.getAssignedTo() == null ? "" : " by " + order.getAssignedTo()));
					}
					order.setAssignedTo(user);
				}

				// eliberez celelalte transporturi NEW ale mele de acelasi tip
				for (TransportOrder other : new TransportOrderRepository(session)
						.findByStatusAndAssignedToOrderByIdAsc(user, List.of(TransportOrderStatus.NEW))) {
					if (!other.getId().equals(id) && isTote(other) == isTote(order)) {
						other.setAssignedTo(null);
					}
				}
				setReturnValue(toResponse(order));
			}
		}.run();
	}

	public void release(Long id, String user) {
		new WithSessionAndTransaction<Void>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				TransportOrder order = find(session, id);
				if (user.equals(order.getAssignedTo()) && order.getStatus() == TransportOrderStatus.NEW) {
					order.setAssignedTo(null);
				}
			}
		}.run();
	}

	private TransportOrder ownedOrder(Session session, Long id, String user) {
		TransportOrder order = find(session, id);
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
		return new WithSessionAndTransaction<TransportOrderResponse>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				TransportOrder order = ownedOrder(session, id, user);
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
					boolean wasAtTpt1 = Constants.TPT1.equals(order.getLoadUnit().getLocation().getName());
					moveLoadUnit(order.getLoadUnit(), order.getTarget());
					order.setStatus(TransportOrderStatus.FINISHED);
					if (wasAtTpt1) {
						releaseTPT1();
					}
				}
				setReturnValue(toResponse(order));
			}
		}.run();
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
		return new WithSessionAndTransaction<TransportOrderResponse>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				TransportOrder order = ownedOrder(session, id, user);

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
				setReturnValue(toResponse(order));
			}
		}.run();
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

	public TransportOrderResponse createTransportOrder(Session session, LoadUnit loadUnit, Location target) {
		TransportOrder order = new TransportOrder(loadUnit.getLocation(), target, TransportOrderStatus.NEW, loadUnit);
		new TransportOrderRepository(session).insert(order);
		return toResponse(order);
	}
}
