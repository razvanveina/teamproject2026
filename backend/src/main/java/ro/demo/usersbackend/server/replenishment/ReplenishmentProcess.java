package ro.demo.usersbackend.server.replenishment;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.TransportOrder;
import ro.demo.usersbackend.entity.TransportOrderStatus;
import ro.demo.usersbackend.server.process.AbstractProcess;
import ro.demo.usersbackend.server.session.ServerSessionFactory;

// muta load unit-urile din HBW care au stoc rezervat la DECANTING (si pentru ro automate)
public class ReplenishmentProcess extends AbstractProcess {
	private static final Logger Log = LogManager.getLogger("replenishment");
	private static final String DECANTING = "DECANTING";
	private static final List<TransportOrderStatus> OPEN = List.of(TransportOrderStatus.NEW,
			TransportOrderStatus.ACTIVE);

	public ReplenishmentProcess() {
		super("replenishmentProcess");
	}

	public static void main(String[] args) {
		new ReplenishmentProcess().start();
	}

	@Override
	protected void doAction() {
		try {
			SessionFactory sessionFactory = ServerSessionFactory.getInstance();

			// load unit-uri din HBW (culoar > 0) cu stoc rezervat si fara transport order
			// deschis
			List<Long> loadUnitIds = sessionFactory.fromTransaction(session -> session.createQuery("""
					select distinct u.id from StorageLocation l
					join l.loadUnit u
					join u.stockItems s
					where l.aisle > 0 and s.reservation is not null
					and not exists (select t.id from TransportOrder t where t.loadUnit = u and t.status in :open)
					""", Long.class).setParameterList("open", OPEN).getResultList());

			loadUnitIds.forEach(loadUnitId -> process(sessionFactory, loadUnitId));
		} catch (Exception e) {
			Log.error("cannot scan the storage locations: {}", e.toString());
		}
	}

	private static void process(SessionFactory sessionFactory, Long loadUnitId) {
		try {
			sessionFactory.inTransaction(session -> replenish(session, session.find(LoadUnit.class, loadUnitId)));
		} catch (Exception e) {
			Log.error("load unit {} not processed {}", loadUnitId, e.toString());
		}
	}

	private static void replenish(Session session, LoadUnit loadUnit) {
		Location decanting = session.createQuery("from Location l where l.name = :name", Location.class)
				.setParameter("name", DECANTING).uniqueResultOptional().orElse(null);
		if (decanting == null) {
			Log.error("location {} is missing", DECANTING);
			return;
		}

		session.persist(new TransportOrder(loadUnit.getLocation(), decanting, TransportOrderStatus.NEW, loadUnit));
		Log.info("load unit {} goes from {} to {}", loadUnit.getBarcode(), loadUnit.getLocation().getName(), DECANTING);
	}
}