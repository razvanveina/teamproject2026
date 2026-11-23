package ro.demo.usersbackend.server.mfs;

import org.hibernate.Session;

import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.TransportOrder;
import ro.demo.usersbackend.entity.TransportOrderStatus;
import ro.demo.usersbackend.messaging.MfsMessages;
import ro.demo.usersbackend.messaging.MfsMessages.Arrived;

public class TPT1Handler extends PointHandler {
	public TPT1Handler() {
		super(Constants.TPT1);
	}

	// tote-ul cu TO spre o locatie din afara benzii (PICK) iese din simulator
	// il punem in locatia tinta, terminam TO-ul si dupa commit eliberam TPT1
	@Override
	public void handle(String telegram) throws Exception {
		super.handle(telegram);

		String barcode = MfsMessages.fromJson(telegram, Arrived.class).loadUnit();
		boolean delivered = new WithSessionAndTransaction<Boolean>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				setReturnValue(deliver(session, barcode));
			}
		}.run();

		if (delivered) {
			StationControl.getInstance().release(Constants.TPT1);
		}
	}

	private boolean deliver(Session session, String barcode) {
		LoadUnit loadUnit = session.createNamedQuery("LoadUnit.findByBarcode", LoadUnit.class)
				.setParameter("barcode", barcode).uniqueResultOptional().orElse(null);
		if (loadUnit == null) {
			return false;
		}

		for (TransportOrder order : session
				.createNamedQuery("TransportOrder.findActiveByLoadUnit", TransportOrder.class)
				.setParameter("loadUnit", loadUnit).setParameter("active", TransportOrderStatus.ACTIVE)
				.getResultList()) {
			if (!Constants.CONVEYOR_POINTS.contains(order.getTarget().getName())) {
				loadUnit.setLocation(order.getTarget());
				order.setStatus(TransportOrderStatus.FINISHED);
				log.info("{}: load unit {} delivered to {}, transport order {} finished", Constants.TPT1, barcode,
						order.getTarget().getName(), order.getId());
				return true;
			}
		}
		return false;
	}
}