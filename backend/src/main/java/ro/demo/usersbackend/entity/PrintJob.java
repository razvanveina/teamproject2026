package ro.demo.usersbackend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;

@Entity
public class PrintJob {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "printer_id", nullable = false)
	private Printer printer;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private DocumentType documentType;

	private LocalDateTime printedAt;

	@Lob
	@Column(nullable = false)
	private String data;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private PrintJobStatus status;

	public PrintJob() {
		super();
	}

	public PrintJob(Printer printer, DocumentType documentType, String data) {
		super();
		this.printer = printer;
		this.documentType = documentType;
		this.data = data;
		this.status = PrintJobStatus.NEW;
	}

	public Long getId() {
		return id;
	}

	public Printer getPrinter() {
		return printer;
	}

	public void setPrinter(Printer printer) {
		this.printer = printer;
	}

	public DocumentType getDocumentType() {
		return documentType;
	}

	public void setDocumentType(DocumentType documentType) {
		this.documentType = documentType;
	}

	public LocalDateTime getPrintedAt() {
		return printedAt;
	}

	public void setPrintedAt(LocalDateTime printedAt) {
		this.printedAt = printedAt;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public PrintJobStatus getStatus() {
		return status;
	}

	public void setStatus(PrintJobStatus status) {
		this.status = status;
	}
}
