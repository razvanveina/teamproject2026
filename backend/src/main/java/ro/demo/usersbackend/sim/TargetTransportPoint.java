package ro.demo.usersbackend.sim;

import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;

public class TargetTransportPoint extends ServerPoint {

	private JMSConsumer consumer;

	public TargetTransportPoint(String name, int x, int y) {
		super(name, x, y);
	}

	public TargetTransportPoint(String name, int x, int y, boolean labelBelow) {
		super(name, x, y, labelBelow);
	}

	@Override
	protected void connected(JMSContext context) {
		consumer = context.createConsumer(context.createQueue(name));
	}

	@Override
	protected void step() throws Exception {
		if (isEmpty()) {
			pull();
			Thread.sleep(100);
			return;
		}

		if (consumer.receive(100) != null) {
			synchronized (this) {
				loadUnit = null;
			}
		}
	}

	private synchronized boolean isEmpty() {
		return loadUnit == null;
	}

	private synchronized void pull() {
		SimLoadUnit arrivingLoadUnit = nextArrivingLoadUnit();
		if (arrivingLoadUnit == null) {
			return;
		}
		loadUnit = arrivingLoadUnit;
		announceLU(arrivingLoadUnit);
	}

	@Override
	public SimLoadUnit getAvailableLU() {
		return null;
	}
}
