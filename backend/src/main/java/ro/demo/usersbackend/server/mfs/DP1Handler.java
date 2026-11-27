package ro.demo.usersbackend.server.mfs;

import java.util.List;

import org.hibernate.Session;

import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.RetrievalOrder;
import ro.demo.usersbackend.entity.StockItem;
import ro.demo.usersbackend.entity.TransportOrder;
import ro.demo.usersbackend.entity.TransportOrderStatus;
import ro.demo.usersbackend.repository.LoadUnitRepository;
import ro.demo.usersbackend.repository.LocationRepository;
import ro.demo.usersbackend.repository.TransportOrderRepository;
import ro.demo.usersbackend.service.PickingConfig;

public class DP1Handler extends DecisionPointHandler {
	public DP1Handler() {
		super(Constants.DP1);
	}

	// 0 = DP1 -> PTTS1 (C7) ; 1 = DP1 -> IP2 (C2)
	// in statie intra doar tote-ul care are TO spre PTTS1
	@Override
	protected int decide(String loadUnitBarcode, String target) {
//		return decideForStation(loadUnit, target, Constants.PTTS1);

		return new WithSessionAndTransaction<Integer>() {

			@Override
			protected void executeBusinessLogic(Session session) {
				boolean sendToPick = false;
				LocationRepository locationRepository = new LocationRepository(session);
				TransportOrderRepository transportOrderRepository = new TransportOrderRepository(session);

				LoadUnit loadUnit = new LoadUnitRepository(session).findByBarcode(loadUnitBarcode).get();

				List<TransportOrder> to = transportOrderRepository.findActiveByLoadUnit(loadUnit);

				if (to.size() > 1) {
					mfsLog.warn("Multiple active transport orders for load unit " + loadUnitBarcode);
				}

				if (!Constants.DP1.equals(to.get(0).getTarget().getName())) {
					setReturnValue(1);
					return;
				}

				PickingConfig config = PickingConfig.getBySourceLocation(Constants.DP1);
				Location dp = locationRepository.findByName(Constants.DP1).get();
				Location station = locationRepository.findByName(config.getPickingTargetLocation()).get();
				Location nextPoint = locationRepository.findByName(config.getTargetLocationAfterConfirm()).get();

				for (StockItem item : loadUnit.getStockItems()) {
					RetrievalOrder order = item.getReservation().getRetrievalOrderLine().getOrder();
					if (order.getPickingLocation() == null) {
						order.setPickingLocation(station);
						sendToPick = true;
						break;
					} else if (config.getPickingTargetLocation().equals(order.getPickingLocation().getName())) {
						sendToPick = true;
						break;
					}
				}

				Location target;
				if (sendToPick) {
					target = station;
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