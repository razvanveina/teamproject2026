package ro.demo.usersbackend.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.entity.DocumentType;
import ro.demo.usersbackend.entity.PrintJob;
import ro.demo.usersbackend.entity.PrintJobStatus;
import ro.demo.usersbackend.entity.Printer;
import ro.demo.usersbackend.entity.PrinterType;
import ro.demo.usersbackend.entity.RetrievalOrder;
import ro.demo.usersbackend.entity.RetrievalOrderLine;
import ro.demo.usersbackend.repository.PrintJobRepository;
import ro.demo.usersbackend.repository.PrinterRepository;

@Service
public class PrintJobService {
	private static final Logger log = LogManager.getLogger("application");
	private final ObjectMapper jsonMapper = new ObjectMapper();

	public void createShippingLabel(RetrievalOrder order, String cartonBarcode) {
		new WithSessionAndTransaction<Void>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				PrinterRepository printerRepository = new PrinterRepository(session);
				PrintJobRepository printJobRepository = new PrintJobRepository(session);

				Printer pdfPrinter = printerRepository.findFirstByType(PrinterType.PDF).orElseThrow(() -> {
					log.error("Printing: no PDF printer found, no shipping label for order {}", order.getOrderId());
					return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
							"No PDF printer is configured, the shipping label cannot be printed");
				});

				List<Map<String, Object>> labelLines = new ArrayList<>();
				for (RetrievalOrderLine orderLine : order.getLines()) {
					Map<String, Object> labelLine = new LinkedHashMap<>();
					labelLine.put(Constants.ARTICLE, orderLine.getArticle().getBarcode());
					labelLine.put(Constants.QUANTITY, orderLine.getQuantity());
					labelLines.add(labelLine);
				}

				Map<String, Object> labelData = new LinkedHashMap<>();
				labelData.put(Constants.ORDER_ID, order.getOrderId());
				labelData.put(Constants.DESTINATION, order.getDestination().getName());
				labelData.put(Constants.CARTON, cartonBarcode);
				labelData.put(Constants.LINES, labelLines);

				try {
					PrintJob printJob = new PrintJob(pdfPrinter, DocumentType.SHIPPING_LABEL,
							jsonMapper.writeValueAsString(labelData));
					printJobRepository.insert(printJob);
					log.info("Printing: shipping label job created for order {} on {}", order.getOrderId(),
							pdfPrinter.getName());
				} catch (JsonProcessingException e) {
					log.error("Printing: cannot create shipping label for order {}: {}", order.getOrderId(),
							e.toString());
					throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
							"The shipping label for order " + order.getOrderId() + " cannot be created");
				}
			}
		}.run();
	}

	public void resetPrintJobForReprint(PrintJob printJob) {
		new WithSessionAndTransaction<Void>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				PrintJobRepository printJobRepository = new PrintJobRepository(session);

				printJob.setStatus(PrintJobStatus.NEW);
				printJob.setPrintedAt(null);
				printJobRepository.save(printJob);
				log.info("Printing: job {} set back to NEW for reprint on {}", printJob.getId(),
						printJob.getPrinter().getName());
			}
		}.run();
	}

	public PrintJob findLatestPrintJobForOrder(String orderId) {
		return new WithSessionAndTransaction<PrintJob>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				PrintJobRepository printJobRepository = new PrintJobRepository(session);
				setReturnValue(printJobRepository.findLatestByOrderId(orderId).orElse(null));
			}
		}.run();
	}
}
