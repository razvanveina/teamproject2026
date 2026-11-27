package ro.demo.usersbackend.server.mfs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.TransportOrder;
import ro.demo.usersbackend.entity.TransportOrderStatus;
import ro.demo.usersbackend.messaging.MfsMessages;
import ro.demo.usersbackend.messaging.MfsMessages.Arrived;

public abstract class PointHandler implements TelegramHandler {
	private static final Logger log = LogManager.getLogger(PointHandler.class);

	private final String point;

	protected PointHandler(String point) {
		this.point = point;
	}

	// actualizam locatia lu-ului ajuns intr-un anumit punct
	// se termina TO cand ajunge pe punct
	@Override
	public void handle(String telegram) throws Exception {
		Arrived arrived = MfsMessages.fromJson(telegram, Arrived.class);

		new WithSessionAndTransaction<>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				arrive(session, arrived.loadUnit());
			}
		}.run();
	}

	private void arrive(Session session, String barcode) {
		Location location = session.createNamedQuery(Location.BY_NAME, Location.class).setParameter("name", point)
				.uniqueResultOptional().orElse(null);
		LoadUnit loadUnit = session.createNamedQuery(LoadUnit.BY_BARCODE, LoadUnit.class)
				.setParameter("barcode", barcode).uniqueResultOptional().orElse(null);

		if (location == null || loadUnit == null) {
			log.warn("{}: load unit {} or location not found", point, barcode);
			return;
		}

		loadUnit.setLocation(location);
		log.info("{}: load unit {} arrived", point, loadUnit.getBarcode());

		for (TransportOrder order : session
				.createNamedQuery(TransportOrder.FIND_ACTIVE_BY_LOAD_UNIT_AND_TARGET, TransportOrder.class)
				.setParameter("loadUnit", loadUnit).setParameter("target", location)
				.setParameter("active", TransportOrderStatus.ACTIVE).getResultList()) {
			order.setStatus(TransportOrderStatus.FINISHED);
			log.info("{}: transport order {} for load unit {} finished", point, order.getId(), loadUnit.getBarcode());
		}
	}
}