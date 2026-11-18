package ro.demo.usersbackend.sim;

public class InfoPoint extends ServerPoint {

	private static final long DWELL_TIME = 1000;

	private long arrivedAt;

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
					arrivedAt = currentSimulationTime();
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
		if (currentSimulationTime() - arrivedAt < DWELL_TIME) {
			return null;
		}
		String s = barcode;
		barcode = null;
		return s;
	}
}