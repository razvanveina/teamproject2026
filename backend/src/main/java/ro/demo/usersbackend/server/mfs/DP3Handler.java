package ro.demo.usersbackend.server.mfs;

import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.server.session.ServerSessionFactory;

public class DP3Handler extends DecisionPointHandler {

	public DP3Handler() {
		super(Constants.DP3);
	}

	// 0 = DP3 -> DP1 (C10) ; 1 = DP3 -> EMPTY (C11)
	@Override
	protected int decide(String loadUnit, String target) throws Exception {
		if (target == null) {
			long stockItems = ServerSessionFactory.getInstance()
					.fromTransaction(session -> session.createNamedQuery("StockItem.countByLoadUnitBarcode", Long.class)
							.setParameter("barcode", loadUnit).getSingleResult());
			return stockItems == 0 ? 1 : 0;
		}
		return Constants.PTTT1.equals(target) ? 1 : 0;
	}
}