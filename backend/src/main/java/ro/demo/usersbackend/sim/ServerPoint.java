package ro.demo.usersbackend.sim;

import java.util.ArrayList;
import java.util.List;

import org.apache.activemq.ActiveMQConnectionFactory;

import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import ro.demo.usersbackend.messaging.Broker;
import ro.demo.usersbackend.messaging.MfsMessages;
import ro.demo.usersbackend.messaging.MfsMessages.Arrived;

public abstract class ServerPoint extends Entity {

	protected SimLoadUnit loadUnit;
	private Long transportOrderId;
	private JMSContext context;
	private Queue outQueue;
	private TimeKeeper timeKeeper = TimeKeeper.getInstance();

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
				if (timeKeeper.isPaused()) {
					Thread.sleep(50);
					continue;
				}
				LOCK.readLock().lock();
				try {
					step();
				} finally {
					LOCK.readLock().unlock();
				}
			}
		} catch (Exception e) {
			System.out.println("Exception " + e);
		}
	}

	@Override
	public synchronized List<SimLoadUnit> getLoadUnits() {
		return loadUnit == null ? List.of() : List.of(loadUnit);
	}

	protected boolean isReady() {
		return false;
	}

	@Override
	public synchronized EntityState snapshot() {
		return new EntityState(name, new ArrayList<>(getLoadUnits()), transportOrderId, isReady());
	}

	@Override
	public synchronized void restore(EntityState state) {
		loadUnit = state.loadUnits().isEmpty() ? null : state.loadUnits().get(0);
		transportOrderId = state.transportOrderId();
	}

	public Long getTransportOrderId() {
		return transportOrderId;
	}

	public void setTransportOrderId(Long transportOrderId) {
		this.transportOrderId = transportOrderId;
	}

	// anuntam serveru ca lu a ajuns intr o anumita locatie
	protected void announceLU(SimLoadUnit arrivedLoadUnit) {
		put(MfsMessages.toJson(new Arrived("ARRIVED", name, arrivedLoadUnit.barcode())));
	}
}
