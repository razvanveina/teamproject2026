package ro.demo.usersbackend.server.mfs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.entity.LoadUnitType;
import ro.demo.usersbackend.messaging.MfsMessages;
import ro.demo.usersbackend.messaging.MfsMessages.Created;
import ro.demo.usersbackend.repository.LoadUnitRepository;
import ro.demo.usersbackend.service.LoadUnitService;

public class LUCHandler implements TelegramHandler {
	private static final Logger log = LogManager.getLogger(LUCHandler.class);

	private final LoadUnitService loadUnitService = new LoadUnitService();

	@Override
	public void handle(String telegram) throws Exception {
		Created created = MfsMessages.fromJson(telegram, Created.class);
		if (isKnownLoadUnit(created.loadUnit())) {
			log.info("{}: load unit {} added in the simulator already exists in the backend", created.point(),
					created.loadUnit());
			return;
		}

		loadUnitService.createEmptyLoadUnit(created.loadUnit(), LoadUnitType.valueOf(created.loadUnitType()));
		log.info("{}: load unit {} added in the simulator, created in the backend", created.point(),
				created.loadUnit());
	}

	private boolean isKnownLoadUnit(String barcode) {
		return new WithSessionAndTransaction<Boolean>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				setReturnValue(new LoadUnitRepository(session).findByBarcode(barcode).isPresent());
			}
		}.run();
	}
}
