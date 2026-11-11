package ro.demo.usersbackend.sim;

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
				announceLU(s);
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