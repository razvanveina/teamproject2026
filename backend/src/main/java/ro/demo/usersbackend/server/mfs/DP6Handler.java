package ro.demo.usersbackend.server.mfs;

import org.hibernate.Session;

import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.server.picking.StationChooser;

public class DP6Handler extends DecisionPointHandler {
	public DP6Handler() {
		super(Constants.DP6);
	}

	@Override
	protected int decide(String loadUnit, String target) throws Exception {
		if (target != null) {
			return Constants.PTTT2.equals(target) ? 0 : 1;
		}
		return new WithSessionAndTransaction<Integer>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				long items = session.createNamedQuery("StockItem.countByLoadUnitBarcode", Long.class)
						.setParameter("barcode", loadUnit).getSingleResult();
				if (items > 0) {
					setReturnValue(1);
					return;
				}
				long onTheWay = session.createNamedQuery("LoadUnit.countOthersByLocationName", Long.class)
						.setParameter("name", Constants.DP6).setParameter("barcode", loadUnit).getSingleResult();
				boolean free = onTheWay == 0 && StationChooser.isTargetFree(session, Constants.PTTT2);
				setReturnValue(free ? 0 : 1);
			}
		}.run();
	}
}