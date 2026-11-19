package ro.demo.usersbackend.sim;

import ro.demo.usersbackend.common.Constants;

public class InfoPoint extends ServerPoint {

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
			SimLoadUnit arrivingLoadUnit = e.getAvailableLU(this);
			if (arrivingLoadUnit != null) {
				synchronized (this) {
					loadUnit = arrivingLoadUnit;
					arrivedAt = currentSimulationTime();
				}
				announceLU(arrivingLoadUnit);
				return;
			}
		}
	}

	private synchronized boolean isEmpty() {
		return loadUnit == null;
	}

	@Override

	public synchronized SimLoadUnit getAvailableLU() {
		if (currentSimulationTime() - arrivedAt < Constants.INFO_POINT_DWELL) {
			return null;
		}
		SimLoadUnit leavingLoadUnit = loadUnit;
		loadUnit = null;
		return leavingLoadUnit;
	}
}
