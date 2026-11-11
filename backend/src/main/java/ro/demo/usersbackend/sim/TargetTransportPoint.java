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
				barcode = null;
			}
		}
	}

	private synchronized boolean isEmpty() {
		return barcode == null;
	}

	private synchronized void pull() {
		for (Entity e : getSource()) {
			String s = e.getAvailableLU(this);
			if (s != null) {
				barcode = s;
				announceLU(s);
				return;
			}
		}
	}

	@Override
	public String getAvailableLU() {
		return null;
	}
}
