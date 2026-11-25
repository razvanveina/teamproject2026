package ro.demo.usersbackend.sim;

import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.messaging.MfsMessages;
import ro.demo.usersbackend.messaging.MfsMessages.AckGet;

public class DecisionPoint extends ServerPoint {

	private Entity decision;
	private long askedAt;
	private boolean isHeld;
	private JMSConsumer consumer;
	private TimeKeeper timeKeeper = TimeKeeper.getInstance();

	public DecisionPoint(String name, int x, int y) {
		super(name, x, y);
	}

	public DecisionPoint(String name, int x, int y, boolean labelBelow) {
		super(name, x, y, labelBelow);
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
				if (Constants.HOLD.equals(reply)) {
					hold();
					return;
				}
				isHeld = false;
				decision = getTarget().get(toIndex(reply));
				log.info(name + ": " + loadUnit.barcode() + " reply " + reply + ", goes to " + decision.getName());
			} else if (isHeld) {
				if (timeKeeper.currentSimulationTime() - askedAt >= Constants.HOLD_RETRY_INTERVAL) {
					askedAt = timeKeeper.currentSimulationTime();
					announceLU(loadUnit);
				}
			} else if (timeKeeper.currentSimulationTime() - askedAt >= Constants.DECISION_TIMEOUT) {
				// fara raspuns (ex. backend repornit): intrebam din nou, nu alegem noi drumul
				askedAt = timeKeeper.currentSimulationTime();
				announceLU(loadUnit);
				log.info(name + ": " + loadUnit.barcode() + " no reply, asking again");
			}
		}
	}

	private void hold() {
		if (!isHeld) {
			log.error(name + ": " + loadUnit.barcode() + " held by the server at " + name);
		}
		isHeld = true;
		askedAt = timeKeeper.currentSimulationTime();
	}

	private int toIndex(String reply) {
		try {
			int index = Integer.parseInt(reply);
			if (index >= 0 && index < getTarget().size()) {
				return index;
			}
		} catch (NumberFormatException e) {
		}
		log.info(name + ": invalid reply " + reply + ", using 0");
		return 0;
	}

	private void pull() {
		SimLoadUnit arrivingLoadUnit = nextArrivingLoadUnit();
		if (arrivingLoadUnit == null) {
			return;
		}
		synchronized (this) {
			loadUnit = arrivingLoadUnit;
			decision = null;
			isHeld = false;
			askedAt = timeKeeper.currentSimulationTime();
		}
		announceLU(arrivingLoadUnit);
		log.info(arrivingLoadUnit.barcode() + " arrived at " + name);
		put(MfsMessages.toJson(new AckGet("DECIDE", null, arrivingLoadUnit.barcode())));
	}

	@Override
	protected synchronized boolean isHoldingLoadUnit() {
		return isHeld && loadUnit != null;
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
		isHeld = false;
		askedAt = timeKeeper.currentSimulationTime();
	}
}