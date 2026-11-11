package ro.demo.usersbackend.sim;

import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import ro.demo.usersbackend.packagemessaging.MfsMessages;
import ro.demo.usersbackend.packagemessaging.MfsMessages.AckGet;

public class DecisionPoint extends ServerPoint {

	private static final long TIMEOUT = 5000;

	private Entity decision;
	private long askedAt;
	private JMSConsumer consumer;

	public DecisionPoint(String name) {
		super(name);
	}

	@Override
	protected void connected(JMSContext context) {
		consumer = context.createConsumer(context.createQueue(name));
	}

	@Override
	protected void step() throws JMSException {
		Message message = consumer.receive(100);

		if (isEmpty()) {
			pull();
			return;
		}

		synchronized (this) {
			if (decision != null) {
				return;
			}
			if (message instanceof TextMessage text) {
				String reply = text.getText().trim();
				decision = getTarget().get(toIndex(reply));
				System.out.println(name + ": " + barcode + " reply " + reply + ", goes to " + decision.getName());
			} else if (System.currentTimeMillis() - askedAt >= TIMEOUT) {
				decision = getTarget().get(0);
				System.out.println(name + ": " + barcode + " no reply, goes to " + decision.getName());
			}
		}
	}

	private int toIndex(String reply) {
		try {
			int index = Integer.parseInt(reply);
			if (index >= 0 && index < getTarget().size()) {
				return index;
			}
		} catch (NumberFormatException e) {
		}
		System.out.println(name + ": invalid reply " + reply + ", using 0");
		return 0;
	}

	private void pull() {
		for (Entity e : getSource()) {
			String s = e.getAvailableLU(this);
			if (s != null) {
				synchronized (this) {
					barcode = s;
					decision = null;
					askedAt = System.currentTimeMillis();
				}
				announceLU(s);
				put(MfsMessages.toJson(new AckGet("DECIDE", null, s)));
				return;
			}
		}
	}

	private synchronized boolean isEmpty() {
		return barcode == null;
	}

	@Override
	public synchronized String getAvailableLU(Entity asking) {
		if (barcode == null || decision == null || asking != decision) {
			return null;
		}
		String s = barcode;
		barcode = null;
		decision = null;
		return s;
	}

	@Override
	public String getAvailableLU() {
		return null;
	}
}