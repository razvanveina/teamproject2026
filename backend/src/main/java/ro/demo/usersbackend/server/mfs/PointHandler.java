package ro.demo.usersbackend.server.mfs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.packagemessaging.MfsMessages;
import ro.demo.usersbackend.packagemessaging.MfsMessages.Arrived;
import ro.demo.usersbackend.server.session.ServerSessionFactory;

public abstract class PointHandler implements TelegramHandler {
	public static final Logger log = LogManager.getLogger("application");

	private final String point;

	protected PointHandler(String point) {
		this.point = point;
	}

	// actualizam locatia lu-ului ajuns intr-un anumit punct
	@Override
	public void handle(String telegram) throws Exception {
		Arrived arrived = MfsMessages.fromJson(telegram, Arrived.class);

		ServerSessionFactory.getInstance().inTransaction(session -> {
			Location location = session.createQuery("from Location l where l.name = :name", Location.class)
					.setParameter("name", point).uniqueResultOptional().orElse(null);
			LoadUnit loadUnit = session.createQuery("from LoadUnit lu where lu.barcode = :barcode", LoadUnit.class)
					.setParameter("barcode", arrived.loadUnit()).uniqueResultOptional().orElse(null);
			
			if (location == null || loadUnit == null) {
				log.warn("{}: load unit {} or location not found", point, arrived.loadUnit());
				return;
			}
			
			loadUnit.setLocation(location);
			log.info("{}: load unit {} arrived", point, loadUnit.getBarcode());
		});
	}
}
