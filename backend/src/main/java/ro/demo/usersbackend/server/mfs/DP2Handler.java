package ro.demo.usersbackend.server.mfs;

import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.server.session.ServerSessionFactory;

public class DP2Handler extends DecisionPointHandler {
	public DP2Handler() {
		super(Constants.DP2);
	}

	// 0 = DP2 -> TPT1 (C5) ; 1 = DP2 -> IP1 (C4), bucla
	@Override
	protected int decide(String loadUnit) throws Exception {
		return ServerSessionFactory.getInstance().fromTransaction(session -> {
			long stockItems = session.createNamedQuery("StockItem.countReservedByLoadUnitBarcode", Long.class)
					.setParameter("barcode", loadUnit).getSingleResult();
			long automatic = session.createNamedQuery("StockItem.countAutomaticByLoadUnitBarcode", Long.class)
					.setParameter("barcode", loadUnit).getSingleResult();
			return (stockItems == 0 || automatic > 0) ? 1 : 0;
		});
	}
}