package ro.demo.usersbackend.sim;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
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
	private boolean isEdited;

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

	public boolean isAt(Point position) {
		return x == position.x && y == position.y;
	}

	public void moveTo(Point position) {
		x = position.x;
		y = position.y;
		isEdited = true;
	}

	public PointLayout layout() {
		return new PointLayout(name, x, y);
	}

	public void applyLayout(PointLayout layout) {
		x = layout.x();
		y = layout.y();
		isEdited = true;
	}

	public boolean isEdited() {
		return isEdited;
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
		try (JMSContext context = new ActiveMQConnectionFactory(Broker.CLIENT_URL).createContext()) {
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
	public synchronized boolean addLoadUnit(SimLoadUnit addedLoadUnit, Sim sim) {
		if (!super.addLoadUnit(addedLoadUnit, sim)) { // if false return false
			return false;
		}

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
				put(MfsMessages
						.toJson(new Created(Constants.CREATED, name, addedLoadUnit.barcode(), addedLoadUnit.type())));
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

	@Override
	public synchronized void clear() {
		pendingLoadUnit = null;
		super.clear();
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
		g.setColor(isHoldingLoadUnit() ? Constants.HELD_POINT_COLOR : Color.WHITE);
		g.fill(pointBox);
		g.setColor(Color.BLACK);
		g.setStroke(new BasicStroke((float) borderWidth));
		g.draw(new Rectangle2D.Double(boxLeft + borderWidth / 2, boxTop + borderWidth / 2,
				SimWindow.POINT - borderWidth, SimWindow.POINT - borderWidth));
		g.setFont(SimWindow.NAME);
		int nameWidth = g.getFontMetrics().stringWidth(name);
		if (name.equals("EMPTY") || name.equals("DP3") || name.equals(Constants.IP3) || name.equals(Constants.IP8)) {
			g.drawString(name, x - h - 4 - nameWidth, y + 4);
		} else if (labelBelow) {
			g.drawString(name, x - nameWidth / 2, y + h + 13);
		} else {
			g.drawString(name, x - nameWidth / 2, y - h - 4);
		}
		tote(g, loadUnit, x, y);

		return pointBox;
	}

	protected boolean isHoldingLoadUnit() {
		return false;
	}

	@Override
	public boolean hasLoadUnit(String barcode) {
		if (loadUnit == null) {
			return false;
		}

		if (loadUnit.barcode().equals(barcode)) {
			return true;
		}
		return false;
	}
}
