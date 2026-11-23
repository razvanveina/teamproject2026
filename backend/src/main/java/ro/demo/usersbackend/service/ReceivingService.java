package ro.demo.usersbackend.service;

import java.util.List;

import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.dto.FinishReceivingRequest;
import ro.demo.usersbackend.dto.PackageOption;
import ro.demo.usersbackend.dto.PackageResponse;
import ro.demo.usersbackend.dto.ReceivingOrderLineResponse;
import ro.demo.usersbackend.dto.ReceivingOrderResponse;
import ro.demo.usersbackend.dto.ScannedPackage;
import ro.demo.usersbackend.entity.ArticlePackagingUnitType;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.ReceivingOrder;
import ro.demo.usersbackend.entity.ReceivingOrderLine;
import ro.demo.usersbackend.entity.ReceivingOrderLineStatus;
import ro.demo.usersbackend.entity.ReceivingOrderStatus;
import ro.demo.usersbackend.entity.StorageLocation;
import ro.demo.usersbackend.repository.ArticlePackagingUnitTypeRepository;
import ro.demo.usersbackend.repository.ReceivingRepository;
import ro.demo.usersbackend.repository.StorageLocationRepository;

@Service
public class ReceivingService {

	private final LoadUnitService loadUnitService;
	private final TransportOrderService transportOrderService;

	public ReceivingService() {
		loadUnitService = new LoadUnitService();
		transportOrderService = new TransportOrderService();
	}

	public List<PackageOption> findPackageOptions(String orderId) {
		return new WithSessionAndTransaction<List<PackageOption>>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				List<String> articleBarcodes = findOrder(session, orderId).getLines().stream()
						.filter(l -> l.getStatus() != ReceivingOrderLineStatus.FINISHED)
						.map(l -> l.getArticle().getBarcode()).toList();

				setReturnValue(
						new ArticlePackagingUnitTypeRepository(session).findByArticleBarcodeIn(articleBarcodes).stream()
								.map(p -> new PackageOption(p.getBarcode(), p.getArticle().getBarcode(),
										p.getArticle().getDescription(), p.getPackagingUnitType().getName(),
										p.getPackagingUnitType().getQuantity()))
								.toList());
			}
		}.run();
	}

	public ReceivingOrderResponse findByOrderId(String orderId) {
		return new WithSessionAndTransaction<ReceivingOrderResponse>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				setReturnValue(toResponse(findOrder(session, orderId)));
			}
		}.run();
	}

	public PackageResponse findPackage(String barcode) {
		ArticlePackagingUnitType pack = new WithSessionAndTransaction<ArticlePackagingUnitType>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				setReturnValue(findPackageEntity(session, barcode));
			}
		}.run();

		return new PackageResponse(pack.getArticle().getBarcode(), pack.getPackagingUnitType().getName(),
				pack.getPackagingUnitType().getQuantity());
	}

	public ReceivingOrderResponse updateOrder(FinishReceivingRequest request) {
		if (request.packages() == null || request.packages().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No packages were scanned");
		}
		for (ScannedPackage scanned : request.packages()) {
			if (scanned.packages() <= 0) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"The number of packages must be greater than 0");
			}
		}

		return new WithSessionAndTransaction<ReceivingOrderResponse>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				ReceivingOrder order = findOrder(session, request.orderId());

				for (ScannedPackage scanned : request.packages()) {
					ArticlePackagingUnitType pack = findPackageEntity(session, scanned.packageBarcode());

					ReceivingOrderLine line = order.getLines().stream()
							.filter(l -> l.getArticle().getBarcode().equals(pack.getArticle().getBarcode())).findFirst()
							.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
									"The article of package " + scanned.packageBarcode() + " is not in the order"));

					int received = line.getReceivedQuantity()
							+ scanned.packages() * pack.getPackagingUnitType().getQuantity();
					if (received > line.getQuantity()) {
						throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
								"Too many pieces received for article " + line.getArticle().getBarcode());
					}

					line.setReceivedQuantity(received);
					if (received == line.getQuantity()) {
						line.setStatus(ReceivingOrderLineStatus.FINISHED);
					}
				}

				if (order.getLines().stream().allMatch(l -> l.getStatus() == ReceivingOrderLineStatus.FINISHED)) {
					order.setStatus(ReceivingOrderStatus.FINISHED);
				}

				LoadUnit loadUnit = loadUnitService.createLoadUnit(session, request.packages());
				StorageLocation target = new StorageLocationRepository(session)
						.findClosestFreeSlot(Constants.OPEN_TRANSPORT_STATUSES)
						.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
								"No free storage location available"));
				transportOrderService.createTransportOrder(session, loadUnit, target);

				setReturnValue(toResponse(order));
			}
		}.run();
	}

	private ReceivingOrder findOrder(Session session, String orderId) {
		return new ReceivingRepository(session).findByOrderId(orderId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Cannot find receiving order with the id: " + orderId));
	}

	private ArticlePackagingUnitType findPackageEntity(Session session, String barcode) {
		return new ArticlePackagingUnitTypeRepository(session).findByBarcode(barcode)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Cannot find package with barcode " + barcode));
	}

	private ReceivingOrderResponse toResponse(ReceivingOrder order) {
		List<ReceivingOrderLineResponse> lines = order.getLines().stream()
				.map(line -> new ReceivingOrderLineResponse(line.getId(), line.getArticle().getDescription(),
						line.getQuantity(), line.getReceivedQuantity(), line.getArticle().getBarcode(),
						line.getStatus().toString()))
				.toList();
		return new ReceivingOrderResponse(order.getOrderId(), order.getStatus().toString(), lines);
	}
}