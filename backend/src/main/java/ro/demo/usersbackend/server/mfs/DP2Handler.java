package ro.demo.usersbackend.server.mfs;

import java.util.List;

import org.hibernate.Session;

import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.StockItem;
import ro.demo.usersbackend.entity.TransportOrder;
import ro.demo.usersbackend.entity.TransportOrderStatus;
import ro.demo.usersbackend.repository.LoadUnitRepository;
import ro.demo.usersbackend.repository.LocationRepository;
import ro.demo.usersbackend.repository.TransportOrderRepository;

public class DP2Handler extends DecisionPointHandler {
	public DP2Handler() {
		super(Constants.DP2);
	}

	// 0 = DP2 -> TPT1 (C5) ; 1 = DP2 -> IP1 (C4), bucla
	// iese la TPT1 daca TO-ul e spre TPT1 sau spre o locatie din afara benzii (ex.
	// PICK), altfel (PTTS, PTTT) mai face o tura
	// fara TO: gol sau cu stoc pentru un order automat -> bucla, altfel TPT1
	@Override
	protected int decide(String loadUnitBarcode, String target) {

		return new WithSessionAndTransaction<Integer>() {

			@Override
			protected void executeBusinessLogic(Session session) {
				boolean sendToStrage = true;
				LocationRepository locationRepository = new LocationRepository(session);
				TransportOrderRepository transportOrderRepository = new TransportOrderRepository(session);

				LoadUnit loadUnit = new LoadUnitRepository(session).findByBarcode(loadUnitBarcode).get();

				List<TransportOrder> to = transportOrderRepository.findActiveByLoadUnit(loadUnit);

				if (to.size() > 1) {
					mfsLog.warn("Multiple active transport orders for load unit " + loadUnitBarcode);
				}

				Location dp = locationRepository.findByName(Constants.DP2).get();
				Location storage = locationRepository.findByName(Constants.TPT1).get();
				Location nextPoint = locationRepository.findByName(Constants.DP1).get();
				Location empty = locationRepository.findByName(Constants.EMPTY).get();

				if (loadUnit.getStockItems().size() == 0) {
					new TransportOrderRepository(session)
							.save(new TransportOrder(dp, empty, TransportOrderStatus.ACTIVE, loadUnit));
					setReturnValue(0);
					return;
				}

				for (StockItem item : loadUnit.getStockItems()) {
					if (item.getReservation() != null) {
						sendToStrage = false;
					}
				}

				Location target;
				if (sendToStrage) {
					target = storage;
					setReturnValue(0);
				} else {
					target = nextPoint;
					setReturnValue(1);
				}
				new TransportOrderRepository(session)
						.save(new TransportOrder(dp, target, TransportOrderStatus.ACTIVE, loadUnit));
			}
		}.run();
	}
}