package ro.demo.usersbackend.sim;

import ro.demo.usersbackend.common.Constants;

public class InfoPoint extends ServerPoint {

	private long arrivedAt;
	private TimeKeeper timeKeeper = TimeKeeper.getInstance();

	public InfoPoint(String name, int x, int y) {
		super(name, x, y);
	}

	public InfoPoint(String name, int x, int y, boolean labelBelow) {
		super(name, x, y, labelBelow);
	}

	@Override
	protected void step() throws InterruptedException {
		if (isEmpty()) {
			pull();
		}
		Thread.sleep(100);
	}

	private void pull() {
		SimLoadUnit arrivingLoadUnit = nextArrivingLoadUnit();
		if (arrivingLoadUnit == null) {
			return;
		}
		synchronized (this) {
			loadUnit = arrivingLoadUnit;
			arrivedAt = timeKeeper.currentSimulationTime();
		}
		announceLU(arrivingLoadUnit);
	}

	private synchronized boolean isEmpty() {
		return loadUnit == null;
	}

	@Override

	public synchronized SimLoadUnit getAvailableLU() {
		if (timeKeeper.currentSimulationTime() - arrivedAt < Constants.INFO_POINT_DWELL) {
			return null;
		}
		SimLoadUnit leavingLoadUnit = loadUnit;
		loadUnit = null;
		return leavingLoadUnit;
	}
}
