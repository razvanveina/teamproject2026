package ro.demo.usersbackend.server.mfs;

import org.hibernate.Session;

import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;

public class DP3Handler extends DecisionPointHandler {

	public DP3Handler() {
		super(Constants.DP3);
	}

	// 0 = DP3 -> DP1 (C10) ; 1 = DP3 -> EMPTY (C11)
	@Override
	protected int decide(String loadUnit, String target) {
		if (target != null) {
			return Constants.PTTT1.equals(target) ? 1 : 0;
		}

		long stockItems = new WithSessionAndTransaction<Long>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				setReturnValue(session.createNamedQuery("StockItem.countByLoadUnitBarcode", Long.class)
						.setParameter("barcode", loadUnit).getSingleResult());
			}
		}.run();
		return stockItems == 0 ? 1 : 0;
	}
}