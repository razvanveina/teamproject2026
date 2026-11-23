package ro.demo.usersbackend.sim;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.messaging.Broker;
import ro.demo.usersbackend.messaging.MfsMessages;
import ro.demo.usersbackend.messaging.MfsMessages.Arrived;
import ro.demo.usersbackend.messaging.MfsMessages.Created;

public abstract class ServerPoint extends Entity {

	protected final Logger log = LogManager.getLogger("application");
	protected SimLoadUnit loadUnit;
	private SimLoadUnit pendingLoadUnit;
	private Long transportOrderId;
	private JMSContext context;
	private Queue outQueue;
	private TimeKeeper timeKeeper = TimeKeeper.getInstance();

	private boolean labelBelow;
	private int x, y;

	public ServerPoint(String name, int x, int y) {
		super(name);
		this.x = x;
		this.y = y;
		labelBelow = false;
	}

	public ServerPoint(String name, int x, int y, boolean labelBelow) {
		super(name);
		this.x = x;
		this.y = y;
		this.labelBelow = labelBelow;
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
				put(MfsMessages.toJson(
						new Created(Constants.CREATED, name, addedLoadUnit.barcode(), addedLoadUnit.type())));
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

	public Shape draw(Graphics2D g) {
		int h = SimWindow.POINT / 2;
		double boxLeft = x - SimWindow.POINT / 2.0;
		double boxTop = y - SimWindow.POINT / 2.0;
		double borderWidth = (SimWindow.CONV - (SimWindow.CONV - 5)) / 2.0;
		Rectangle2D pointBox = new Rectangle2D.Double(boxLeft, boxTop, SimWindow.POINT, SimWindow.POINT);
		g.setColor(Color.WHITE);
		g.fill(pointBox);
		g.setColor(Color.BLACK);
		g.setStroke(new BasicStroke((float) borderWidth));
		g.draw(new Rectangle2D.Double(boxLeft + borderWidth / 2, boxTop + borderWidth / 2,
				SimWindow.POINT - borderWidth, SimWindow.POINT - borderWidth));
		g.setFont(SimWindow.NAME);
		int nameWidth = g.getFontMetrics().stringWidth(name);
		if (name.equals("EMPTY") || name.equals("DP3") || name.equals(Constants.IP3)) {
			g.drawString(name, x - h - 4 - nameWidth, y + 4);
		} else if (labelBelow) {
			g.drawString(name, x - nameWidth / 2, y + h + 13);
		} else {
			g.drawString(name, x - nameWidth / 2, y - h - 4);
		}
		tote(g, loadUnit, x, y);

		return pointBox;
	}

}
