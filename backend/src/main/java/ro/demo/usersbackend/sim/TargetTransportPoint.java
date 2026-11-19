package ro.demo.usersbackend.sim;

import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;

public class TargetTransportPoint extends ServerPoint {

	private JMSConsumer consumer;

	public TargetTransportPoint(String name) {
		super(name);
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
		for (Entity e : getSource()) {
			SimLoadUnit arrivingLoadUnit = e.getAvailableLU(this);
			if (arrivingLoadUnit != null) {
				loadUnit = arrivingLoadUnit;
				announceLU(arrivingLoadUnit);
				return;
			}
		}
	}

	@Override
	public SimLoadUnit getAvailableLU() {
		return null;
	}
}
