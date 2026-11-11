package ro.demo.usersbackend.server.mfs;

import ro.demo.usersbackend.server.session.ServerSessionFactory;

public class DP3Handler extends DecisionPointHandler {

	public DP3Handler() {
		super("DP3");
	}

	// 0 = DP3 -> DP1 (C10) ; 1 = DP3 -> EMPTY (C11) (lu gol, fara stock items)
	@Override
	protected int decide(String loadUnit) throws Exception {
		long stockItems = ServerSessionFactory.getInstance().fromTransaction(session -> session
				.createQuery("select count(s) from StockItem s where s.loadUnit.barcode = :barcode", Long.class)
				.setParameter("barcode", loadUnit).getSingleResult());
		return stockItems == 0 ? 1 : 0;
	}
}