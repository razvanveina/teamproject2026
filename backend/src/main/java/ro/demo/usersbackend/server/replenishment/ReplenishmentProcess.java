package ro.demo.usersbackend.server.replenishment;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.common.Queries;
import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.LoadUnitType;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.StorageLocation;
import ro.demo.usersbackend.entity.TransportOrder;
import ro.demo.usersbackend.entity.TransportOrderStatus;
import ro.demo.usersbackend.server.process.AbstractProcess;

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
		new WithSessionAndTransaction<>() {

			@Override
			protected void executeBusinessLogic(Session session) {
				List<Long> loadUnitIds = session.createNamedQuery(Queries.FIND_RESERVED_IDS_IN_STORAGE, Long.class)
						.setParameterList("open", Constants.OPEN_TRANSPORT_STATUSES).getResultList();

				loadUnitIds.forEach(loadUnitId -> process(session, loadUnitId, true));
			}
		}.run();
	}

	// paleti in DECANTING fara niciun stock item rezervat si fara to deschis
	private void backToStorage() {
		new WithSessionAndTransaction<>() {

			@Override
			protected void executeBusinessLogic(Session session) {
				List<Long> loadUnitIds = session.createNamedQuery(Queries.FIND_FREE_PALLET_IDS_IN_DECANTING, Long.class)
						.setParameter("pallet", LoadUnitType.PALLET).setParameter("decanting", Constants.DECANTING)
						.setParameterList("open", Constants.OPEN_TRANSPORT_STATUSES).getResultList();

				loadUnitIds.forEach(loadUnitId -> process(session, loadUnitId, false));
			}
		}.run();
	}

	private void process(Session session, Long loadUnitId, boolean toDecanting) {
		LoadUnit loadUnit = session.find(LoadUnit.class, loadUnitId);
		if (toDecanting) {
			replenish(session, loadUnit);
		} else {
			store(session, loadUnit);
		}
	}

	private void replenish(Session session, LoadUnit loadUnit) {
		Location decanting = session.createNamedQuery(Location.BY_NAME, Location.class)
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
		StorageLocation target = session.createNamedQuery(Queries.FIND_FREE_STORAGE_LOCATIONS, StorageLocation.class)
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