package ro.demo.usersbackend.sim;

import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;

public class StationPoint extends ServerPoint {

	private boolean released;
	private JMSConsumer consumer;

	public StationPoint(String name, int x, int y) {
		super(name, x, y);
	}

	public StationPoint(String name, int x, int y, boolean labelBelow) {
		super(name, x, y, labelBelow);
	}

	@Override
	protected void connected(JMSContext context) {
		consumer = context.createConsumer(context.createQueue(name));
	}

	@Override
	protected void step() throws JMSException {
		Message message = consumer.receive(100);

		synchronized (this) {
			if (loadUnit == null) {
				pull();
				return;
			}
			if (message != null && !released) {
				released = true;
				log.info(name + ": " + loadUnit.barcode() + " released");
			}
		}
	}

	private void pull() {
		SimLoadUnit arrivingLoadUnit = nextArrivingLoadUnit();
		if (arrivingLoadUnit == null) {
			return;
		}
		loadUnit = arrivingLoadUnit;
		released = false;
		log.info(arrivingLoadUnit.barcode() + " arrived at " + name);
		announceLU(arrivingLoadUnit);
	}

	@Override
	public synchronized SimLoadUnit getAvailableLU() {
		if (loadUnit == null || !released) {
			return null;
		}
		SimLoadUnit leavingLoadUnit = loadUnit;
		loadUnit = null;
		released = false;
		return leavingLoadUnit;
	}

	@Override
	protected boolean isReady() {
		return released;
	}

	@Override
	public synchronized void restore(EntityState state) {
		super.restore(state);
		released = state.ready();
	}
}
