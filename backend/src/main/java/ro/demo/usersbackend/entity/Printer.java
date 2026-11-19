package ro.demo.usersbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Printer {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String name;

	private String ip;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private PrinterType type;

	public Printer() {
		super();
	}

	public Printer(String name, String ip, PrinterType type) {
		super();
		this.name = name;
		this.ip = ip;
		this.type = type;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public PrinterType getType() {
		return type;
	}

	public void setType(PrinterType type) {
		this.type = type;
	}
}
