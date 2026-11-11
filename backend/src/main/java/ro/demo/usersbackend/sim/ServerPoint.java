package ro.demo.usersbackend.sim;

import org.apache.activemq.ActiveMQConnectionFactory;

import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import ro.demo.usersbackend.packagemessaging.Broker;
import ro.demo.usersbackend.packagemessaging.MfsMessages;
import ro.demo.usersbackend.packagemessaging.MfsMessages.Arrived;

public abstract class ServerPoint extends Entity {

	public static final String IN_QUEUE = MfsMessages.REQUEST_QUEUE;
	public static final String OUT_QUEUE = MfsMessages.RESPONSE_QUEUE;

	protected String barcode;
	private Long transportOrderId;
	private JMSContext context;
	private Queue outQueue;

	public ServerPoint(String name) {
		super(name);
	}

	public void put(String message) {
		context.createProducer().send(outQueue, message);
		System.out.println(message);
	}

	protected void connected(JMSContext context) {
	}

	protected abstract void step() throws Exception;

	@Override
	public void run() {
		try (JMSContext context = new ActiveMQConnectionFactory(Broker.URL).createContext()) {
			this.context = context;
			outQueue = context.createQueue(OUT_QUEUE);
			connected(context);

			while (true) {
				step();
			}
		} catch (Exception e) {
			System.out.println("Exception " + e);
		}
	}

	public Long getTransportOrderId() {
		return transportOrderId;
	}

	public void setTransportOrderId(Long transportOrderId) {
		this.transportOrderId = transportOrderId;
	}
	
	// anuntam serveru ca lu a ajuns intr o anumita locatie
	protected void announceLU(String barcode) {
		put(MfsMessages.toJson(new Arrived("ARRIVED", name, barcode)));
	}
}
