package ro.demo.usersbackend.sim;

import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.packagemessaging.MfsMessages;
import ro.demo.usersbackend.packagemessaging.MfsMessages.AckGet;

public class DecisionPoint extends ServerPoint {

	private Entity decision;
	private long askedAt;
	private JMSConsumer consumer;
	private TimeKeeper timeKeeper = TimeKeeper.getInstance();

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
				System.out.println(
						name + ": " + loadUnit.barcode() + " reply " + reply + ", goes to " + decision.getName());
			} else if (timeKeeper.currentSimulationTime() - askedAt >= Constants.DECISION_TIMEOUT) {
				decision = getTarget().get(0);
				System.out.println(name + ": " + loadUnit.barcode() + " no reply, goes to " + decision.getName());
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
			SimLoadUnit arrivingLoadUnit = e.getAvailableLU(this);
			if (arrivingLoadUnit != null) {
				synchronized (this) {
					loadUnit = arrivingLoadUnit;
					decision = null;
					askedAt = timeKeeper.currentSimulationTime();
				}
				announceLU(arrivingLoadUnit);
				System.out.println(arrivingLoadUnit.barcode() + " arrived at " + name);
				put(MfsMessages.toJson(new AckGet("DECIDE", null, arrivingLoadUnit.barcode())));
				return;
			}
		}
	}

	private synchronized boolean isEmpty() {
		return loadUnit == null;
	}

	@Override
	public synchronized SimLoadUnit getAvailableLU(Entity asking) {
		if (loadUnit == null || decision == null || asking != decision) {
			return null;
		}
		SimLoadUnit leavingLoadUnit = loadUnit;
		loadUnit = null;
		decision = null;
		return leavingLoadUnit;
	}

	@Override
	public SimLoadUnit getAvailableLU() {
		return null;
	}

	@Override
	public synchronized void restore(EntityState state) {
		super.restore(state);
		decision = null;
		askedAt = timeKeeper.currentSimulationTime();
	}
}
