package ro.demo.usersbackend.server.replenishment;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

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
		toDecanting();
		backToStorage();
	}

	private void toDecanting() {
		try {
			SessionFactory sessionFactory = ServerSessionFactory.getInstance();

			// load unit-uri din HBW (culoar > 0) cu stoc rezervat si fara to deschis
			List<Long> loadUnitIds = sessionFactory.fromTransaction(session -> session.createQuery("""
					select distinct u.id from StorageLocation l
					join l.loadUnit u
					join u.stockItems s
					where l.aisle > 0 and s.reservation is not null
					and not exists (select t.id from TransportOrder t where t.loadUnit = u and t.status in :open)
					""", Long.class).setParameterList("open", OPEN).getResultList());

			loadUnitIds.forEach(loadUnitId -> process(sessionFactory, loadUnitId, true));
		} catch (Exception e) {
			Log.error("cannot scan the storage locations: {}", e.toString());
		}
	}

	// paleti in DECANTING fara niciun stock item rezervat si fara to deschis
	private void backToStorage() {
		try {
			SessionFactory sessionFactory = ServerSessionFactory.getInstance();

			List<Long> loadUnitIds = sessionFactory.fromTransaction(session -> session.createQuery("""
					select u.id from LoadUnit u
					where u.type = :pallet and u.location.name = :decanting
					and not exists (select s.id from StockItem s where s.loadUnit = u and s.reservation is not null)
					and not exists (select t.id from TransportOrder t where t.loadUnit = u and t.status in :open)
					""", Long.class).setParameter("pallet", LoadUnitType.PALLET).setParameter("decanting", DECANTING)
					.setParameterList("open", OPEN).getResultList());

			loadUnitIds.forEach(loadUnitId -> process(sessionFactory, loadUnitId, false));
		} catch (Exception e) {
			Log.error("cannot scan the decanting location: {}", e.toString());
		}
	}

	private static void process(SessionFactory sessionFactory, Long loadUnitId, boolean toDecanting) {
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

	// prima locatie libera din HBW spre care nu vine niciun to deschis
	private static void store(Session session, LoadUnit loadUnit) {
		StorageLocation target = session.createQuery("""
				from StorageLocation l
				where l.aisle > 0 and l.loadUnit is null
				and l.id not in (select t.target.id from TransportOrder t where t.status in :open)
				order by l.x asc
				""", StorageLocation.class).setParameterList("open", OPEN).setMaxResults(1).uniqueResultOptional()
				.orElse(null);
		if (target == null) {
			Log.warn("no free HBW location, load unit {} stays in {}", loadUnit.getBarcode(), DECANTING);
			return;
		}

		session.persist(new TransportOrder(loadUnit.getLocation(), target, TransportOrderStatus.NEW, loadUnit));
		Log.info("load unit {} goes from {} to {}", loadUnit.getBarcode(), DECANTING, target.getName());
	}
}