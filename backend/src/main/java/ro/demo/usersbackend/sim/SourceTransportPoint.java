package ro.demo.usersbackend.sim;

import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import ro.demo.usersbackend.packagemessaging.MfsMessages;
import ro.demo.usersbackend.packagemessaging.MfsMessages.AckGet;
import ro.demo.usersbackend.packagemessaging.MfsMessages.Get;

public class SourceTransportPoint extends ServerPoint {

	private long loadedAt;
	private boolean acknowledged;
	private JMSConsumer consumer;

	public SourceTransportPoint(String name) {
		super(name);
	}

	@Override
	protected void connected(JMSContext context) {
		consumer = context.createConsumer(context.createQueue(MfsMessages.REQUEST_QUEUE));
	}

	public synchronized void get(String barcode) {
		get(barcode, null);
	}

	public synchronized void get(String barcode, Long transportOrderId) {
		this.barcode = barcode;
		this.setTransportOrderId(transportOrderId);
		loadedAt = System.currentTimeMillis();
		acknowledged = false;
		System.out.println(barcode + " loaded");
	}

	@Override
	protected void step() throws JMSException, InterruptedException {
		if (isEmpty()) {
			Message message = consumer.receive(100);
			if (message instanceof TextMessage text) {
				Get request = MfsMessages.fromJson(text.getText(), Get.class);
				get(request.loadUnit(), request.transportOrderId());
			}
		} else {
			Thread.sleep(100);
		}

		synchronized (this) {
			if (barcode != null && !acknowledged && System.currentTimeMillis() - loadedAt >= 2000) {
				acknowledged = true;
				put(MfsMessages.toJson(new AckGet("ACK_GET", getTransportOrderId(), barcode)));
				announceLU(barcode);
			}
		}
	}

	private synchronized boolean isEmpty() {
		return barcode == null;
	}

	@Override
	public synchronized String getAvailableLU() {
		if (barcode == null || !acknowledged) {
			return null;
		}
		String s = barcode;
		barcode = null;
		return s;
	}

	@Override
	protected boolean isReady() {
		return acknowledged;
	}

	@Override
	public synchronized void restore(EntityState state) {
		super.restore(state);
		loadedAt = System.currentTimeMillis();
		acknowledged = state.ready();
	}
}
