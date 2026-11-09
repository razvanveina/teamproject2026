package ro.demo.usersbackend.sim;

import org.apache.activemq.ActiveMQConnectionFactory;

import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import jakarta.jms.TextMessage;
import ro.demo.usersbackend.packagemessaging.Broker;

public class TransportPoint extends Entity {

	public static final String IN_QUEUE = "SIM_QUEUE";
	public static final String OUT_QUEUE = "TEST_QUEUE";

	private String barcode;
	private long loadedAt;
	private boolean acknowledged;
	private JMSContext context;
	private Queue outQueue;

	public TransportPoint(String name) {
		super(name);
	}

	public synchronized void get(String barcode) {
		this.barcode = barcode;
		loadedAt = System.currentTimeMillis();
		acknowledged = false;
		System.out.println(barcode + " loaded");
	}

	public void put(String message) {
		context.createProducer().send(outQueue, message);
		System.out.println(message);
	}

	@Override
	public void run() {
		try (JMSContext context = new ActiveMQConnectionFactory(Broker.URL).createContext()) {
			this.context = context;
			outQueue = context.createQueue(OUT_QUEUE);
			JMSConsumer consumer = context.createConsumer(context.createQueue(IN_QUEUE));

			while (true) {
				Message message = consumer.receive(100);
				if (message instanceof TextMessage text) {
					get(text.getText());
				}

				synchronized (this) {
					if (barcode != null && !acknowledged && System.currentTimeMillis() - loadedAt >= 2000) {
						acknowledged = true;
						put("Acknowledged" + barcode);
					}
				}
			}
		} catch (Exception e) {
			System.out.println(name + " stopped: " + e);
		}
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

}
