package ro.demo.usersbackend.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.entity.DocumentType;
import ro.demo.usersbackend.entity.PrintJob;
import ro.demo.usersbackend.entity.PrintJobStatus;
import ro.demo.usersbackend.entity.Printer;
import ro.demo.usersbackend.entity.PrinterType;
import ro.demo.usersbackend.entity.RetrievalOrder;
import ro.demo.usersbackend.entity.RetrievalOrderLine;

@Service
@Transactional
public class PrintJobService {
	private static final Logger log = LogManager.getLogger("application");
	private final ObjectMapper jsonMapper = new ObjectMapper();

	@PersistenceContext
	private EntityManager entityManager;

	public void createShippingLabel(RetrievalOrder order) {
		List<Printer> pdfPrinters = entityManager.createNamedQuery("Printer.findByType", Printer.class)
				.setParameter("printerType", PrinterType.PDF).setMaxResults(1).getResultList();
		if (pdfPrinters.isEmpty()) {
			log.error("Printing: no PDF printer found, no shipping label for order {}", order.getOrderId());
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
					"No PDF printer is configured, the shipping label cannot be printed");
		}
		Printer pdfPrinter = pdfPrinters.get(0);

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
		labelData.put(Constants.LINES, labelLines);

		try {
			PrintJob printJob = new PrintJob(pdfPrinter, DocumentType.SHIPPING_LABEL,
					jsonMapper.writeValueAsString(labelData));
			entityManager.persist(printJob);
			log.info("Printing: shipping label job created for order {} on {}", order.getOrderId(),
					pdfPrinter.getName());
		} catch (JsonProcessingException e) {
			log.error("Printing: cannot create shipping label for order {}: {}", order.getOrderId(), e.toString());
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
					"The shipping label for order " + order.getOrderId() + " cannot be created");
		}
	}

	public void resetPrintJobForReprint(PrintJob printJob) {
		printJob.setStatus(PrintJobStatus.NEW);
		printJob.setPrintedAt(null);
		log.info("Printing: job {} set back to NEW for reprint on {}", printJob.getId(),
				printJob.getPrinter().getName());
	}

	public PrintJob findLatestPrintJobForOrder(String orderId) {
		List<PrintJob> latestPrintJobs = entityManager.createNamedQuery("PrintJob.findLatestByOrderId", PrintJob.class)
				.setParameter(Constants.PRINT_JOB_ORDER_ID_PARAMETER, orderId).getResultList();
		return latestPrintJobs.isEmpty() ? null : latestPrintJobs.get(0);
	}
}
