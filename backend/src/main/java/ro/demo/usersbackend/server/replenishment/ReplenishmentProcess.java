package ro.demo.usersbackend.server.replenishment;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.LoadUnitType;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.StorageLocation;
import ro.demo.usersbackend.entity.TransportOrder;
import ro.demo.usersbackend.entity.TransportOrderStatus;
import ro.demo.usersbackend.server.process.AbstractProcess;
import ro.demo.usersbackend.server.session.ServerSessionFactory;

// muta load unit-urile din HBW care au stoc rezervat la DECANTING (si pentru ro automate)
//  acm si trimite inapoi in HBW paletii din DECANTING care nu mai au stoc rezervat
public class ReplenishmentProcess extends AbstractProcess {
	private static final Logger Log = LogManager.getLogger("replenishment");

	public ReplenishmentProcess() {
		super("replenishmentProcess");
	}

	public static void main(String[] args) {
		new ReplenishmentProcess().start();
	}

	@Override
	protected void doAction() {
		toDecanting();
		backToStorage();
	}

	private void toDecanting() {
		try {
			SessionFactory sessionFactory = ServerSessionFactory.getInstance();

			// load unit-uri din HBW (culoar > 0) cu stoc rezervat si fara to deschis
			List<Long> loadUnitIds = sessionFactory.fromTransaction(
					session -> session.createNamedQuery("LoadUnit.findReservedIdsInStorage", Long.class)
							.setParameterList("open", Constants.OPEN_TRANSPORT_STATUSES).getResultList());

			loadUnitIds.forEach(loadUnitId -> process(sessionFactory, loadUnitId, true));
		} catch (Exception e) {
			Log.error("cannot scan the storage locations: {}", e.toString());
		}
	}

	// paleti in DECANTING fara niciun stock item rezervat si fara to deschis
	private void backToStorage() {
		try {
			SessionFactory sessionFactory = ServerSessionFactory.getInstance();

			List<Long> loadUnitIds = sessionFactory.fromTransaction(
					session -> session.createNamedQuery("LoadUnit.findFreePalletIdsInDecanting", Long.class)
							.setParameter("pallet", LoadUnitType.PALLET).setParameter("decanting", Constants.DECANTING)
							.setParameterList("open", Constants.OPEN_TRANSPORT_STATUSES).getResultList());

			loadUnitIds.forEach(loadUnitId -> process(sessionFactory, loadUnitId, false));
		} catch (Exception e) {
			Log.error("cannot scan the decanting location: {}", e.toString());
		}
	}

	private void process(SessionFactory sessionFactory, Long loadUnitId, boolean toDecanting) {
		try {
			sessionFactory.inTransaction(session -> {
				LoadUnit loadUnit = session.find(LoadUnit.class, loadUnitId);
				if (toDecanting) {
					replenish(session, loadUnit);
				} else {
					store(session, loadUnit);
				}
			});
		} catch (Exception e) {
			Log.error("load unit {} not processed {}", loadUnitId, e.toString());
		}
	}

	private void replenish(Session session, LoadUnit loadUnit) {
		Location decanting = session.createNamedQuery("Location.findByName", Location.class)
				.setParameter("name", Constants.DECANTING).uniqueResultOptional().orElse(null);
		if (decanting == null) {
			Log.error("location {} is missing", Constants.DECANTING);
			return;
		}

		session.persist(new TransportOrder(loadUnit.getLocation(), decanting, TransportOrderStatus.NEW, loadUnit));
		Log.info("load unit {} goes from {} to {}", loadUnit.getBarcode(), loadUnit.getLocation().getName(),
				Constants.DECANTING);
	}

	// prima locatie libera din HBW spre care nu vine niciun to deschis
	private void store(Session session, LoadUnit loadUnit) {
		StorageLocation target = session
				.createNamedQuery("StorageLocation.findFreeStorageLocations", StorageLocation.class)
				.setParameterList("open", Constants.OPEN_TRANSPORT_STATUSES).setMaxResults(1).uniqueResultOptional()
				.orElse(null);
		if (target == null) {
			Log.warn("no free HBW location, load unit {} stays in {}", loadUnit.getBarcode(), Constants.DECANTING);
			return;
		}

		session.persist(new TransportOrder(loadUnit.getLocation(), target, TransportOrderStatus.NEW, loadUnit));
		Log.info("load unit {} goes from {} to {}", loadUnit.getBarcode(), Constants.DECANTING, target.getName());
	}
}