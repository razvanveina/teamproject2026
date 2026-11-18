package ro.demo.usersbackend.server.mfs;

import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.packagemessaging.MfsMessages;
import ro.demo.usersbackend.packagemessaging.MfsMessages.Arrived;
import ro.demo.usersbackend.server.session.ServerSessionFactory;

public class OUTHandler extends PointHandler {
	public OUTHandler() {
		super(Constants.OUT);
	}

	@Override
	public void handle(String telegram) throws Exception {
		super.handle(telegram);

		String barcode = MfsMessages.fromJson(telegram, Arrived.class).loadUnit();
		long reserved = ServerSessionFactory.getInstance().fromTransaction(session -> session.createQuery(
				"select count(s) from StockItem s where s.loadUnit.barcode = :barcode and s.reservation is not null",
				Long.class).setParameter("barcode", barcode).getSingleResult());

		if (reserved == 0) {
			log.warn("{}: tote {} has nothing to ship, released", Constants.OUT, barcode);
			StationControl.release(Constants.OUT);
		} else {
			log.info("{}: tote {} waits for shipping", Constants.OUT, barcode);
		}
	}
}