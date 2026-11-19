package ro.demo.usersbackend.sim;

import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import ro.demo.usersbackend.messaging.MfsMessages;
import ro.demo.usersbackend.messaging.MfsMessages.AckGet;
import ro.demo.usersbackend.messaging.MfsMessages.Get;

public class SourceTransportPoint extends ServerPoint {

	private long loadedAt;
	private boolean acknowledged;
	private JMSConsumer consumer;
	private TimeKeeper timeKeeper = TimeKeeper.getInstance();

	public SourceTransportPoint(String name) {
		super(name);
	}

	@Override
	protected void connected(JMSContext context) {
		consumer = context.createConsumer(context.createQueue(MfsMessages.REQUEST_QUEUE));
	}

	public synchronized void get(SimLoadUnit requestedLoadUnit) {
		get(requestedLoadUnit, null);
	}

	public synchronized void get(SimLoadUnit requestedLoadUnit, Long transportOrderId) {
		this.loadUnit = requestedLoadUnit;
		this.setTransportOrderId(transportOrderId);
		loadedAt = timeKeeper.currentSimulationTime();
		acknowledged = false;
		System.out.println(requestedLoadUnit.barcode() + " (" + requestedLoadUnit.type() + ") loaded");
	}

	@Override
	protected void step() throws JMSException, InterruptedException {
		if (isEmpty()) {
			Message message = consumer.receive(100);
			if (message instanceof TextMessage text) {
				Get request = MfsMessages.fromJson(text.getText(), Get.class);
				get(new SimLoadUnit(request.loadUnit(), request.loadUnitType()), request.transportOrderId());
			}
		} else {
			Thread.sleep(100);
		}

		synchronized (this) {
			if (loadUnit != null && !acknowledged && timeKeeper.currentSimulationTime() - loadedAt >= 2000) {
				acknowledged = true;
				put(MfsMessages.toJson(new AckGet("ACK_GET", getTransportOrderId(), loadUnit.barcode())));
				announceLU(loadUnit);
			}
		}
	}

	private synchronized boolean isEmpty() {
		return loadUnit == null;
	}

	@Override
	public synchronized SimLoadUnit getAvailableLU() {
		if (loadUnit == null || !acknowledged) {
			return null;
		}
		SimLoadUnit leavingLoadUnit = loadUnit;
		loadUnit = null;
		return leavingLoadUnit;
	}

	@Override
	protected boolean isReady() {
		return acknowledged;
	}

	@Override
	public synchronized void restore(EntityState state) {
		super.restore(state);
		loadedAt = timeKeeper.currentSimulationTime();
		acknowledged = state.ready();
	}
}
