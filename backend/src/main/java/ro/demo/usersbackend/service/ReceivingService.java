package ro.demo.usersbackend.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ro.demo.usersbackend.dto.FinishReceivingRequest;
import ro.demo.usersbackend.dto.PackageOption;
import ro.demo.usersbackend.dto.PackageResponse;
import ro.demo.usersbackend.dto.ReceivingOrderLineResponse;
import ro.demo.usersbackend.dto.ReceivingOrderResponse;
import ro.demo.usersbackend.dto.ScannedPackage;
import ro.demo.usersbackend.entity.ArticlePackagingUnitType;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.ReceivingOrder;
import ro.demo.usersbackend.entity.ReceivingOrderLine;
import ro.demo.usersbackend.entity.ReceivingOrderLineStatus;
import ro.demo.usersbackend.entity.ReceivingOrderStatus;
import ro.demo.usersbackend.repository.ArticlePackagingUnitTypeRepository;
import ro.demo.usersbackend.repository.ReceivingRepository;

@Service
@Transactional
public class ReceivingService {

	private final LoadUnitService loadUnitService;
	private final StorageLocationService storageLocationService;
	private final TransportOrderService transportOrderService;
	private final ArticlePackagingUnitTypeRepository articlePackagingUnitTypeRepository;
	private final ReceivingRepository receivingRepository;

	public ReceivingService(LoadUnitService loadUnitService, StorageLocationService storageLocationService,
			TransportOrderService transportOrderService, ReceivingRepository receivingRepository,
			ArticlePackagingUnitTypeRepository articlePackagingUnitTypeRepository) {
		super();
		this.loadUnitService = loadUnitService;
		this.storageLocationService = storageLocationService;
		this.transportOrderService = transportOrderService;
		this.receivingRepository = receivingRepository;
		this.articlePackagingUnitTypeRepository = articlePackagingUnitTypeRepository;
	}

	@Transactional
	public List<PackageOption> findPackageOptions(String orderId) {
		ReceivingOrder order = receivingRepository.findByOrderId(orderId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Cannot find receiving order with the id: " + orderId));

		List<String> articleBarcodes = order.getLines().stream()
				.filter(l -> l.getStatus() != ReceivingOrderLineStatus.FINISHED).map(l -> l.getArticle().getBarcode())
				.toList();

		return articlePackagingUnitTypeRepository.findByArticleBarcodeIn(articleBarcodes).stream()
				.map(p -> new PackageOption(p.getBarcode(), p.getArticle().getBarcode(),
						p.getArticle().getDescription(), p.getPackagingUnitType().getName(),
						p.getPackagingUnitType().getQuantity()))
				.toList();
	}

	@Transactional(readOnly = true)
	public ReceivingOrderResponse findByOrderId(String orderId) {
		ReceivingOrder order = receivingRepository.findByOrderId(orderId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Cannot find receiving order with the id: " + orderId));

		return new ReceivingOrderResponse(order.getOrderId(), order.getStatus().toString(),
				getResponseLines(order.getLines()));
	}

	@Transactional(readOnly = true)
	public PackageResponse findPackage(String barcode) {
		ArticlePackagingUnitType pack = findPackageEntity(barcode);

		return new PackageResponse(pack.getArticle().getBarcode(), pack.getPackagingUnitType().getName(),
				pack.getPackagingUnitType().getQuantity());
	}

	private ArticlePackagingUnitType findPackageEntity(String barcode) {
		return articlePackagingUnitTypeRepository.findByBarcode(barcode)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Cannot find package with barcode " + barcode));
	}

	private List<ReceivingOrderLineResponse> getResponseLines(List<ReceivingOrderLine> lines) {

		List<ReceivingOrderLineResponse> responseLines = new ArrayList<>();
		for (ReceivingOrderLine line : lines) {
			responseLines.add(
					new ReceivingOrderLineResponse(line.getId(), line.getArticle().getDescription(), line.getQuantity(),
							line.getReceivedQuantity(), line.getArticle().getBarcode(), line.getStatus().toString()));
		}

		return responseLines;
	}

	@Transactional
	public ReceivingOrderResponse updateOrder(FinishReceivingRequest request) {
		if (request.packages() == null || request.packages().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No packages were scanned");
		}

		ReceivingOrder order = receivingRepository.findByOrderId(request.orderId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Cannot find receiving order with the id: " + request.orderId()));

		for (ScannedPackage scanned : request.packages()) {
			if (scanned.packages() <= 0) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"The number of packages must be greater than 0");
			}

			ArticlePackagingUnitType pack = findPackageEntity(scanned.packageBarcode());

			ReceivingOrderLine line = order.getLines().stream()
					.filter(l -> l.getArticle().getBarcode().equals(pack.getArticle().getBarcode())).findFirst()
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
							"The article of package " + scanned.packageBarcode() + " is not in the order"));

			int received = line.getReceivedQuantity() + scanned.packages() * pack.getPackagingUnitType().getQuantity();
			if (received > line.getQuantity()) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Too many pieces received for article " + line.getArticle().getBarcode());
			}

			line.setReceivedQuantity(received);
			if (received == line.getQuantity()) {
				line.setStatus(ReceivingOrderLineStatus.FINISHED);
			}
		}

		boolean allFinished = order.getLines().stream()
				.allMatch(l -> l.getStatus() == ReceivingOrderLineStatus.FINISHED);
		if (allFinished) {
			order.setStatus(ReceivingOrderStatus.FINISHED);
		}

		createTransportOrder(request.packages());

		return new ReceivingOrderResponse(order.getOrderId(), order.getStatus().toString(),
				getResponseLines(order.getLines()));
	}

	private void createTransportOrder(List<ScannedPackage> scannedPackages) {
		LoadUnit loadUnit = loadUnitService.createLoadUnit(scannedPackages);
		Location target = storageLocationService.findClosestEmptyX();
		transportOrderService.createTransportOrder(loadUnit, target);
	}
}