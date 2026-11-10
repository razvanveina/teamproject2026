package ro.demo.usersbackend.sim;

import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;

public class DecisionPoint extends ServerPoint {

	private static final long TIMEOUT = 5000;

	private volatile Entity yesTarget;
	private volatile Entity noTarget;
	private Entity decision;
	private long askedAt;
	private JMSConsumer consumer;

	public DecisionPoint(String name) {
		super(name);
	}

	public void setYesTarget(Entity yesTarget) {
		this.yesTarget = yesTarget;
	}

	public void setNoTarget(Entity noTarget) {
		this.noTarget = noTarget;
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
				decision = "yes".equalsIgnoreCase(reply) ? yesTarget : noTarget;
				System.out.println(name + ": " + barcode + " reply " + reply + ", goes to " + decision.getName());
			} else if (System.currentTimeMillis() - askedAt >= TIMEOUT) {
				decision = noTarget;
				System.out.println(name + ": " + barcode + " no reply, goes to " + decision.getName());
			}
		}
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
				put(name + " " + s);
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

	@Override
	public String toJSON(int id) {
		StringBuilder builder = new StringBuilder();

		builder.append("{" + super.toJSON(id) + ",");
		builder.append("\"barcodes\":[");

		builder.append("\"" + super.barcode + "\"");

		builder.append("]}");
		return builder.toString();
	}
}
