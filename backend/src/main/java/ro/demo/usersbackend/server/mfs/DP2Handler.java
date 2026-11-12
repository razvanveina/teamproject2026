package ro.demo.usersbackend.server.mfs;

import ro.demo.usersbackend.server.session.ServerSessionFactory;

public class DP2Handler extends DecisionPointHandler {
	public DP2Handler() {
		super("DP2");
	}

	// 0 = DP2 -> TPT1 (C5) ; 1 = DP2 -> IP1 (C4)
	@Override
	protected int decide(String loadUnit) throws Exception {
		long stockItems = ServerSessionFactory.getInstance()
				.fromTransaction(session -> session
						.createQuery("select count(s) from StockItem s where s.loadUnit.barcode = :barcode", Long.class)
						.setParameter("barcode", loadUnit).getSingleResult());
		return stockItems == 0 ? 1 : 0;
	}
}