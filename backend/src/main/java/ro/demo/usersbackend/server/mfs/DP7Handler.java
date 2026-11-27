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

public class DP7Handler extends DecisionPointHandler {
	public DP7Handler() {
		super(Constants.DP7);
	}

	@Override
	protected int decide(String loadUnitBarcode, String target) {
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

				if (!Constants.DP7.equals(to.get(0).getTarget().getName())) {
					setReturnValue(1);
					return;
				}

				PickingConfig config = PickingConfig.getBySourceLocation(Constants.DP7);
				Location dp = locationRepository.findByName(Constants.DP7).get();
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