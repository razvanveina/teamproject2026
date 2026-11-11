package ro.demo.usersbackend.sim;

import java.util.List;

import org.apache.activemq.ActiveMQConnectionFactory;

import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import ro.demo.usersbackend.packagemessaging.Broker;
import ro.demo.usersbackend.packagemessaging.MfsMessages;

public abstract class ServerPoint extends Entity {

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
			outQueue = context.createQueue(MfsMessages.RESPONSE_QUEUE);
			connected(context);

			while (true) {
				step();
			}
		} catch (Exception e) {
			System.out.println("Exception " + e);
		}
	}

	@Override
	public synchronized List<String> getLoadUnits() {
		return barcode == null ? List.of() : List.of(barcode);
	}

	public Long getTransportOrderId() {
		return transportOrderId;
	}

	public void setTransportOrderId(Long transportOrderId) {
		this.transportOrderId = transportOrderId;
	}

}
