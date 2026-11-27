package ro.demo.usersbackend.server.printing;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.Barcode128;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.entity.PrintJob;
import ro.demo.usersbackend.entity.PrintJobStatus;
import ro.demo.usersbackend.entity.PrinterType;
import ro.demo.usersbackend.server.process.AbstractProcess;

public class PrintingService extends AbstractProcess {
	private static final Logger log = LogManager.getLogger(PrintingService.class);
	private final ObjectMapper jsonMapper = new ObjectMapper();

	public PrintingService() {
		super("printingService");
	}

	public static void main(String[] args) {
		new PrintingService().start();
	}

	@Override
	protected void doAction() {
		List<Long> newPdfPrintJobIds;
		try {
			newPdfPrintJobIds = new WithSessionAndTransaction<List<Long>>() {
				@Override
				protected void executeBusinessLogic(Session session) {
					setReturnValue(findNewPdfPrintJobIds(session));
				}
			}.run();
		} catch (Exception e) {
			log.error("Printing: cannot scan the print jobs: {}", e.toString());
			return;
		}

		for (Long printJobId : newPdfPrintJobIds) {
			try {
				new WithSessionAndTransaction<>() {
					@Override
					protected void executeBusinessLogic(Session session) {
						printPdfJob(session, printJobId);
					}
				}.run();
			} catch (Exception e) {
				log.error("Printing: print job {} not processed {}", printJobId, e.toString());
			}
		}
	}

	private List<Long> findNewPdfPrintJobIds(Session session) {
		return session.createNamedQuery(PrintJob.FIND_NEW_IDS_BY_PRINTER_TYPE, Long.class)
				.setParameter("status", PrintJobStatus.NEW).setParameter("printerType", PrinterType.PDF)
				.getResultList();
	}

	private void printPdfJob(Session session, Long printJobId) {
		PrintJob printJob = session.find(PrintJob.class, printJobId);
		try {
			Path pdfFile = writeShippingLabelPdf(printJob);
			printJob.setStatus(PrintJobStatus.PRINTED);
			printJob.setPrintedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
			log.info("Printing: print job {} printed on {} to {}", printJobId, printJob.getPrinter().getName(),
					pdfFile);
		} catch (Exception e) {
			printJob.setStatus(PrintJobStatus.ERROR);
			log.error("Printing: print job {} failed {}", printJobId, e.toString());
		}
	}

	private Path writeShippingLabelPdf(PrintJob printJob) throws Exception {
		JsonNode labelData = jsonMapper.readTree(printJob.getData());
		String orderId = labelData.get(Constants.ORDER_ID).asText();

		Path pdfFile = Path.of(Constants.PDF_OUTPUT_FOLDER, Constants.PREFIX + orderId + Constants.PDF_EXTENSION);
		Files.createDirectories(pdfFile.getParent());

		try (OutputStream pdfOutputStream = Files.newOutputStream(pdfFile)) {
			Document document = new Document();
			PdfWriter pdfWriter = PdfWriter.getInstance(document, pdfOutputStream);
			document.open();

			document.add(new Paragraph(Constants.SHIPPING_DOCUMENT_TITLE, new Font(Font.HELVETICA, 18, Font.BOLD)));
			document.add(new Paragraph(Constants.ORDER_ID_TITLE + orderId));
			document.add(new Paragraph(Constants.DESTINATION_TITLE + labelData.get(Constants.DESTINATION).asText()));
			JsonNode carton = labelData.get(Constants.CARTON);
			if (carton != null && !carton.isNull()) {
				document.add(new Paragraph(Constants.CARTON_TITLE + carton.asText()));
			}

			PdfPTable articleTable = new PdfPTable(2);
			articleTable.setSpacingBefore(10);
			articleTable.setSpacingAfter(20);
			articleTable.addCell(Constants.ARTICLE_HEADER);
			articleTable.addCell(Constants.QUANTITY_HEADER);
			for (JsonNode labelLine : labelData.get(Constants.LINES)) {
				articleTable.addCell(labelLine.get(Constants.ARTICLE).asText());
				articleTable.addCell(labelLine.get(Constants.QUANTITY).asText());
			}
			document.add(articleTable);

			Barcode128 orderIdBarcode = new Barcode128();
			orderIdBarcode.setCode(orderId);
			document.add(orderIdBarcode.createImageWithBarcode(pdfWriter.getDirectContent(), null, null));

			document.close();
		}
		return pdfFile;
	}
}
