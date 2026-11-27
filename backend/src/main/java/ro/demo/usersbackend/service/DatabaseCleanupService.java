package ro.demo.usersbackend.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

import ro.demo.usersbackend.common.Queries;
import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;

// butonul Clean database din Testing: sterge datele de lucru, pastreaza datele de baza
// (articole, ambalaje, locatii, destinatii, imprimante, useri) si porneste secventele si ID-urile de la 1
public class DatabaseCleanupService {
	private static final Logger log = LogManager.getLogger("application");

	public synchronized void cleanWorkingData() {
		new WithSessionAndTransaction<Void>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				session.createNamedMutationQuery(Queries.CLEAN_WORKING_DATA).executeUpdate();
			}
		}.run();
		log.info("Database cleanup: working data removed, sequences and ids start from 1");
	}
}