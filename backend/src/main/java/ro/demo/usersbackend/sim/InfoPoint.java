package ro.demo.usersbackend.sim;

import ro.demo.usersbackend.packagemessaging.MfsMessages;
import ro.demo.usersbackend.packagemessaging.MfsMessages.AckGet;

public class InfoPoint extends ServerPoint {

	public InfoPoint(String name) {
		super(name);
	}

	@Override
	protected void step() throws InterruptedException {
		if (isEmpty()) {
			pull();
		}
		Thread.sleep(100);
	}

	private void pull() {
		for (Entity e : getSource()) {
			String s = e.getAvailableLU(this);
			if (s != null) {
				synchronized (this) {
					barcode = s;
				}
				System.out.println(s + " arrived at " + name);
				put(MfsMessages.toJson(new AckGet("INFO", null, s)));
				return;
			}
		}
	}

	private synchronized boolean isEmpty() {
		return barcode == null;
	}

	@Override
	public synchronized String getAvailableLU() {
		String s = barcode;
		barcode = null;
		return s;
	}
}
