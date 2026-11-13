package ro.demo.usersbackend.sim;

import org.apache.activemq.ActiveMQConnectionFactory;

import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import ro.demo.usersbackend.packagemessaging.Broker;
import ro.demo.usersbackend.packagemessaging.MfsMessages;

public class FrontendCommunicationLayer {

	private Sim sim;
	private String barcode;
	private Long transportOrderId;
	private JMSContext context;
	private JMSConsumer consumer;
	private Queue outQueue;

	public FrontendCommunicationLayer() {
		try (JMSContext context = new ActiveMQConnectionFactory(Broker.URL).createContext()) {
			this.context = context;
			outQueue = context.createQueue(MfsMessages.RESPONSE_QUEUE);
			connected(context);

		} catch (Exception e) {
			System.out.println("Exception " + e);
		}
	}

	private void connected(JMSContext context) {
		consumer = context.createConsumer(context.createQueue(MfsMessages.REQUEST_QUEUE));
	}

	public void put(String message, String point) {
		context.createProducer().send(outQueue, message);
		System.out.println(message);
	}

	public synchronized void get(String barcode, Long transportOrderId) {
		this.barcode = barcode;
		this.transportOrderId = transportOrderId;
		System.out.println(barcode + " loaded");
	}
}
