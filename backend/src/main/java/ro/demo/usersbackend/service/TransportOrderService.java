package ro.demo.usersbackend.service;

import java.util.List;
import java.util.Optional;

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
import ro.demo.usersbackend.repository.TransportOrderRepository;

@Service
@Transactional
public class TransportOrderService {

	private static final String DECANTING = "DECANTING";

	private final TransportOrderRepository transportOrderRepository;
	private final LocationRepository locationRepository;

	public TransportOrderService(TransportOrderRepository transportOrderRepository,
			LocationRepository locationRepository) {
		super();
		this.transportOrderRepository = transportOrderRepository;
		this.locationRepository = locationRepository;
	}

	private Location decanting() {
		return locationRepository.findByName(DECANTING)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"Cannot find location with the name " + DECANTING));
	}

	@Transactional(readOnly = true)
	public Optional<TransportOrderResponse> findNextOrder() {
		Location decanting = decanting();
		return transportOrderRepository
				.findFirstByStatusAndSourceNotOrderByIdAsc(TransportOrderStatus.ACTIVE, decanting)
				.or(() -> transportOrderRepository.findFirstByStatusAndSourceNotOrderByIdAsc(TransportOrderStatus.NEW,
						decanting))
				.map(this::toResponse);
	}

	@Transactional(readOnly = true)
	public Optional<TransportOrderResponse> findNextToteOrder() {
		Location decanting = decanting();

		return transportOrderRepository.findFirstByStatusAndSourceOrderByIdAsc(TransportOrderStatus.ACTIVE, decanting)
				.or(() -> transportOrderRepository.findFirstByStatusAndSourceOrderByIdAsc(TransportOrderStatus.NEW,
						decanting))
				.map(this::toResponse);
	}

	@Transactional(readOnly = true)
	public List<TransportOrderResponse> findOpenOrders() {
		return transportOrderRepository.findByStatusAndSourceNotOrderByIdAsc(TransportOrderStatus.NEW, decanting())
				.stream().map(this::toResponse).toList();
	}

	public TransportOrderResponse scan(Long id, String barcode) {
		TransportOrder order = transportOrderRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transport order not found"));
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

	public TransportOrderResponse reportError(Long id, TransportOrderErrorReason reason) {
		TransportOrder order = transportOrderRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transport order not found"));

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

		if (loadUnit.getLocation() instanceof StorageLocation previous) {
			previous.setLoadUnit(null);
			transportOrderRepository.flush();
		}
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
