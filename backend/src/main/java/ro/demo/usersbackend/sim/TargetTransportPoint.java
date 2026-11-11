package ro.demo.usersbackend.sim;

import ro.demo.usersbackend.packagemessaging.MfsMessages;
import ro.demo.usersbackend.packagemessaging.MfsMessages.AckGet;

public class TargetTransportPoint extends ServerPoint {

	public TargetTransportPoint(String name) {
		super(name);
	}

	@Override
	protected void step() throws InterruptedException {
		for (Entity e : getSource()) {
			if (barcode == null) {
				barcode = e.getAvailableLU(this);
			}
		}

		if (barcode != null) {
			put(MfsMessages.toJson(new AckGet("READY", null, barcode)));
			barcode = null;
		}

		Thread.sleep(100);
	}

	@Override
	public String getAvailableLU() {
		return null;
	}
}
