package ro.demo.usersbackend.repository;

import java.util.Optional;

import org.hibernate.Session;

import ro.demo.usersbackend.entity.DestinationToGate;

public class DestinationToGateRepository extends BaseRepository<DestinationToGate, Long> {

	public DestinationToGateRepository(Session session) {
		super(session, DestinationToGate.class);
	}

	public Optional<DestinationToGate> findByGateName(String name) {
		return session.createNamedQuery(DestinationToGate.BY_GATE_NAME, DestinationToGate.class)
				.setParameter("name", name).uniqueResultOptional();
	}

	public Optional<DestinationToGate> findByDestinationName(String name) {
		return session.createNamedQuery(DestinationToGate.BY_DESTINATION_NAME, DestinationToGate.class)
				.setParameter("destination", name).uniqueResultOptional();
	}
}