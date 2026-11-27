package ro.demo.usersbackend.repository;

import java.util.Optional;

import org.hibernate.Session;

import ro.demo.usersbackend.entity.PrintJob;

public class PrintJobRepository extends BaseRepository<PrintJob, Long> {

	public PrintJobRepository(Session session) {
		super(session, PrintJob.class);
	}

	public Optional<PrintJob> findLatestByOrderId(String orderId) {
		return session.createNamedQuery(PrintJob.FIND_LATEST_BY_ORDER_ID, PrintJob.class)
				.setParameter("orderId", orderId).setMaxResults(1).uniqueResultOptional();
	}
}
