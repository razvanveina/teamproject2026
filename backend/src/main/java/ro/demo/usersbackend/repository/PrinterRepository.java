package ro.demo.usersbackend.repository;

import java.util.Optional;

import org.hibernate.Session;

import ro.demo.usersbackend.entity.Printer;
import ro.demo.usersbackend.entity.PrinterType;

public class PrinterRepository extends BaseRepository<Printer, Long> {

	public PrinterRepository(Session session) {
		super(session, Printer.class);
	}

	public Optional<Printer> findFirstByType(PrinterType printerType) {
		return session.createNamedQuery(Printer.BY_TYPE, Printer.class).setParameter("printerType", printerType)
				.setMaxResults(1).uniqueResultOptional();
	}
}
