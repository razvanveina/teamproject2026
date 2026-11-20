package ro.demo.usersbackend.sim;

import java.util.ArrayList;
import java.util.List;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import ro.demo.usersbackend.messaging.Broker;
import ro.demo.usersbackend.messaging.MfsMessages;
import ro.demo.usersbackend.messaging.MfsMessages.Arrived;

public abstract class ServerPoint extends Entity {

	protected final Logger log = LogManager.getLogger("application");
	protected SimLoadUnit loadUnit;
	private SimLoadUnit pendingLoadUnit;
	private Long transportOrderId;
	private JMSContext context;
	private Queue outQueue;
	private TimeKeeper timeKeeper = TimeKeeper.getInstance();

	public ServerPoint(String name) {
		super(name);
	}

	public void put(String message) {
		context.createProducer().send(outQueue, message);
		log.info(message);
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
			log.error("Exception", e);
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
	public synchronized boolean addLoadUnit(SimLoadUnit addedLoadUnit) {
		if (loadUnit != null || pendingLoadUnit != null) {
			return false;
		}
		pendingLoadUnit = addedLoadUnit;
		return true;
	}

	protected SimLoadUnit nextArrivingLoadUnit() {
		synchronized (this) {
			if (pendingLoadUnit != null) {
				SimLoadUnit addedLoadUnit = pendingLoadUnit;
				pendingLoadUnit = null;
				return addedLoadUnit;
			}
		}
		for (Entity sourceEntity : getSource()) {
			SimLoadUnit arrivingLoadUnit = sourceEntity.getAvailableLU(this);
			if (arrivingLoadUnit != null) {
				return arrivingLoadUnit;
			}
		}
		return null;
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
