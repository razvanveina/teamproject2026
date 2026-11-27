package ro.demo.usersbackend.sim;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// gate de la capatul liniei de dupa OUT: cartonul ajunge, anunta serverul (ARRIVED)
// si dupa GATE_DWELL iese din sim, adica a fost incarcat in camion
public class GatePoint extends ServerPoint {

	private static final Logger log = LogManager.getLogger(GatePoint.class);

	private long arrivedAt;
	private TimeKeeper timeKeeper = TimeKeeper.getInstance();

	public GatePoint(String name, int x, int y) {
		super(name, x, y);
	}

	public GatePoint(String name, int x, int y, boolean labelBelow) {
		super(name, x, y, labelBelow);
	}

	@Override
	protected void step() throws InterruptedException {
		// comentat temporar
//		synchronized (this) {
//			if (loadUnit != null && timeKeeper.currentSimulationTime() - arrivedAt >= Constants.GATE_DWELL) {
//				log.info("{} left the conveyor at {}", loadUnit.barcode(), name);
//				loadUnit = null;
//			}
//		}
		if (isEmpty()) {
			pull();
		}
		Thread.sleep(100);
	}

	// ia cartonul de pe banda care coboara la gate si anunta serverul
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

	// gate-ul e capat de linie, nimic nu pleaca mai departe din el
	@Override
	public SimLoadUnit getAvailableLU() {
		return null;
	}

	// dupa restart, cartonul ramas pe gate mai sta GATE_DWELL si apoi iese
	@Override
	public synchronized void restore(EntityState state) {
		super.restore(state);
		arrivedAt = timeKeeper.currentSimulationTime();
	}
}