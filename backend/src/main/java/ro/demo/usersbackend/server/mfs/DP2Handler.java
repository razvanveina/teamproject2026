package ro.demo.usersbackend.server.mfs;

import org.hibernate.Session;

import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;

public class DP2Handler extends DecisionPointHandler {
	public DP2Handler() {
		super(Constants.DP2);
	}

	// 0 = DP2 -> TPT1 (C5) ; 1 = DP2 -> IP1 (C4), bucla
	// spre TPT1 iese, spre orice alt punct (PTTS1, PTTT1) mai face o tura
	// fara TO: gol sau cu stoc pentru un order automat -> bucla, altfel TPT1
	@Override
	protected int decide(String loadUnit, String target) {
		if (target != null) {
			return Constants.TPT1.equals(target) ? 0 : 1;
		}

		return new WithSessionAndTransaction<Integer>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				long stockItems = session.createNamedQuery("StockItem.countByLoadUnitBarcode", Long.class)
						.setParameter("barcode", loadUnit).getSingleResult();
				long automatic = session.createNamedQuery("StockItem.countAutomaticByLoadUnitBarcode", Long.class)
						.setParameter("barcode", loadUnit).getSingleResult();
				setReturnValue((stockItems == 0 || automatic > 0) ? 1 : 0);
			}
		}.run();
	}
}