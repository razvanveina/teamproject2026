package ro.demo.usersbackend.sim;

import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;

public class StationPoint extends ServerPoint {

	private boolean released;
	private JMSConsumer consumer;

	public StationPoint(String name) {
		super(name);
	}

	@Override
	protected void connected(JMSContext context) {
		consumer = context.createConsumer(context.createQueue(name));
	}

	@Override
	protected void step() throws JMSException {
		Message message = consumer.receive(100);

		synchronized (this) {
			if (barcode == null) {
				pull();
				return;
			}
			if (message != null && !released) {
				released = true;
				System.out.println(name + ": " + barcode + " released");
			}
		}
	}

	private void pull() {
		for (Entity e : getSource()) {
			String s = e.getAvailableLU(this);
			if (s != null) {
				barcode = s;
				released = false;
				System.out.println(s + " arrived at " + name);
				announceLU(s);
				return;
			}
		}
	}

	@Override
	public synchronized String getAvailableLU() {
		if (barcode == null || !released) {
			return null;
		}
		String s = barcode;
		barcode = null;
		released = false;
		return s;
	}
}
