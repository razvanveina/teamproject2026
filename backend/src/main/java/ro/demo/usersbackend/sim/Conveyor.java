package ro.demo.usersbackend.sim;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.jms.JMSContext;
import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.messaging.Broker;
import ro.demo.usersbackend.messaging.MfsMessages;
import ro.demo.usersbackend.messaging.MfsMessages.Created;

public class Conveyor extends Entity {

	private static final Logger log = LogManager.getLogger("application");

	public record ToteOnBelt(SimLoadUnit loadUnit, double progress) {
	}

	private final class Tote {
		private final SimLoadUnit loadUnit;
		private int slotIndex;
		private double startPosition;
		private long moveStartedAt;
		private long moveDuration;

		private Tote(SimLoadUnit loadUnit, int slotIndex, double startPosition, long moveStartedAt, long moveDuration) {
			this.loadUnit = loadUnit;
			this.slotIndex = slotIndex;
			this.startPosition = startPosition;
			this.moveStartedAt = moveStartedAt;
			this.moveDuration = moveDuration;
		}
	}

	private long travelTime;
	private int capacity;
	private long slotTravelTime;
	private Deque<Tote> totes;
	private volatile boolean ready;
	private TimeKeeper timeKeeper = TimeKeeper.getInstance();

	private Point labelPositon;
	private int[] pathCoordonates;
	private Shape shape;
	private Shape anchor;

	public Conveyor(String name, long travelTime, int capacity, Point labelPositon, int... xy) {
		super(name);
		this.travelTime = travelTime;
		this.capacity = capacity;
		this.slotTravelTime = capacity > 1 ? travelTime / (capacity - 1) : travelTime;
		this.totes = new ArrayDeque<>();
		ready = true;

		this.labelPositon = labelPositon;
		pathCoordonates = Arrays.stream(xy).toArray();
	}

	public long getTravelTime() {
		return travelTime;
	}

	public int getCapacity() {
		return capacity;
	}

	public synchronized boolean createLU(SimLoadUnit createdLoadUnit) {
		if (!isSlotFree(0, timeKeeper.currentSimulationTime())) {
			return false;
		}
		enterFirstSlot(createdLoadUnit);
		return true;
	}

	@Override
	public boolean addLoadUnit(SimLoadUnit addedLoadUnit) {
		if (!createLU(addedLoadUnit)) {
			return false;
		}
		announceCreated(addedLoadUnit);
		return true;
	}

	private void announceCreated(SimLoadUnit createdLoadUnit) {
		String message = MfsMessages.toJson(
				new Created(Constants.CREATED, name, createdLoadUnit.barcode(), createdLoadUnit.type()));
		try (JMSContext context = new ActiveMQConnectionFactory(Broker.URL).createContext()) {
			context.createProducer().send(context.createQueue(MfsMessages.RESPONSE_QUEUE), message);
		}
		log.info(message);
	}

	@Override
	public void run() {
		try {
			while (!ready) {
				Thread.sleep(10);
			}

			while (true) {
				if (timeKeeper.isPaused()) {
					Thread.sleep(50);
					continue;
				}
				LOCK.readLock().lock();
				try {
					advanceTotes();
					pull();
				} finally {
					LOCK.readLock().unlock();
				}
				Thread.sleep(20);
			}
		} catch (InterruptedException e) {
			return;
		}
	}

	private synchronized void advanceTotes() {
		long currentTime = timeKeeper.currentSimulationTime();
		for (Tote tote : totes) {
			if (hasArrived(tote, currentTime) && tote.slotIndex < capacity - 1
					&& isSlotFree(tote.slotIndex + 1, currentTime)) {
				tote.startPosition = slotPosition(tote.slotIndex);
				tote.slotIndex++;
				tote.moveStartedAt = currentTime;
				tote.moveDuration = slotTravelTime;
			}
		}
	}

	private void pull() {
		synchronized (this) {
			if (!isSlotFree(0, timeKeeper.currentSimulationTime())) {
				return;
			}
		}

		for (Entity e : getSource()) {
			SimLoadUnit arrivingLoadUnit = e.getAvailableLU(this);
			if (arrivingLoadUnit != null) {
				synchronized (this) {
					enterFirstSlot(arrivingLoadUnit);
				}
				return;
			}
		}
	}

	private void enterFirstSlot(SimLoadUnit enteringLoadUnit) {
		long entryDuration = capacity > 1 ? 0 : travelTime;
		totes.addLast(new Tote(enteringLoadUnit, 0, 0.0, timeKeeper.currentSimulationTime(), entryDuration));
	}

	private boolean isSlotFree(int slotIndex, long currentTime) {
		for (Tote tote : totes) {
			if (tote.slotIndex == slotIndex) {
				return false;
			}
			if (tote.slotIndex - 1 == slotIndex && !hasArrived(tote, currentTime)) {
				return false;
			}
		}
		return true;
	}

	private boolean hasArrived(Tote tote, long currentTime) {
		return currentTime - tote.moveStartedAt >= tote.moveDuration;
	}

	private double slotPosition(int slotIndex) {
		return capacity > 1 ? slotIndex / (double) (capacity - 1) : 1.0;
	}

	private double currentPosition(Tote tote, long currentTime) {
		double targetPosition = slotPosition(tote.slotIndex);
		if (tote.moveDuration == 0) {
			return targetPosition;
		}
		double movedFraction = Math.min(1.0, (currentTime - tote.moveStartedAt) / (double) tote.moveDuration);
		return tote.startPosition + (targetPosition - tote.startPosition) * movedFraction;
	}

	@Override
	public synchronized SimLoadUnit getAvailableLU() {
		Tote first = totes.peekFirst();
		if (first == null || first.slotIndex < capacity - 1 || !hasArrived(first, timeKeeper.currentSimulationTime())) {
			return null;
		}
		totes.pollFirst();
		return first.loadUnit;
	}

	@Override
	public synchronized List<SimLoadUnit> getLoadUnits() {
		return totes.stream().map(tote -> tote.loadUnit).toList();
	}

	public synchronized List<ToteOnBelt> getTotesOnBelt() {
		long currentTime = timeKeeper.currentSimulationTime();
		return totes.stream().map(tote -> new ToteOnBelt(tote.loadUnit, currentPosition(tote, currentTime))).toList();
	}

	@Override
	public synchronized void restore(EntityState state) {
		List<SimLoadUnit> loadUnits = state.loadUnits();
		int restoredCount = Math.min(loadUnits.size(), capacity);
		long currentTime = timeKeeper.currentSimulationTime();
		for (int toteIndex = 0; toteIndex < restoredCount; toteIndex++) {
			int slotIndex = capacity - 1 - toteIndex;
			totes.addLast(new Tote(loadUnits.get(toteIndex), slotIndex, slotPosition(slotIndex), currentTime, 0));
		}
		if (loadUnits.size() > capacity) {
			log.warn("{}: snapshot has {} totes but capacity is {}, not restored: {}", name, loadUnits.size(), capacity,
					loadUnits.subList(capacity, loadUnits.size()));
		}
	}

	public Shape draw(Graphics2D g) {
		Path2D beltPath = toPath(trimEndsToPointEdges(pathCoordonates));
		BasicStroke beltStroke = new BasicStroke(SimWindow.CONV, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
		g.setColor(Color.BLACK);
		g.setStroke(beltStroke);
		g.draw(beltPath);
		g.setColor(Color.WHITE);
		g.setStroke(new BasicStroke(SimWindow.CONV - 5, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
		g.draw(toPath(pathCoordonates));

		g.setColor(Color.GRAY);
		g.setStroke(new BasicStroke(1.75f));

		arrow(g, pathCoordonates);

		g.setColor(Color.BLACK);
		g.setFont(SimWindow.CONV_LABEL);
		g.drawString(name, (int) labelPositon.getX(), (int) labelPositon.getY());

//		Conveyor conveyor = (Conveyor) byName.get(name);
		double margin = SimWindow.POINT / 2.0 + SimWindow.TOTE / 2.0 + 2;
		double usable = length(pathCoordonates) - 2 * margin;
		for (Conveyor.ToteOnBelt toteOnBelt : getTotesOnBelt()) {
			double[] totePosition = pointAt(pathCoordonates, margin + toteOnBelt.progress() * usable);
			tote(g, toteOnBelt.loadUnit(), totePosition[0], totePosition[1]);
		}

		shape = beltStroke.createStrokedShape(beltPath);
		return shape;
	}

	public void drawAnchors(Graphics2D g) {
		for (int i = 0; i < pathCoordonates.length / 2; i++) {
			g.setColor(Color.RED);
//			Rectangle2D anchor = new Rectangle2D(pathCoordonates[i * 2] - 5, pathCoordonates[i * 2 + 1] - 5, 10, 10);
			g.drawRoundRect(pathCoordonates[i * 2] - 5, pathCoordonates[i * 2 + 1] - 5, 10, 10, 7, 7);
		}
	}

	// toPath and trimEnds are both used for drawing the conveyor and points so they
	// overlap well and dont stick out
	private Path2D toPath(int[] coordinates) {
		Path2D path = new Path2D.Double();
		path.moveTo(coordinates[0], coordinates[1]);
		for (int i = 2; i < coordinates.length; i += 2) {
			path.lineTo(coordinates[i], coordinates[i + 1]);
		}
		return path;
	}

	private int[] trimEndsToPointEdges(int[] centerLine) {
		int[] trimmedCoordinates = centerLine.clone();
		moveTowardsNextPoint(trimmedCoordinates, 0, 2, SimWindow.POINT / 2.0);
		moveTowardsNextPoint(trimmedCoordinates, centerLine.length - 2, centerLine.length - 4, SimWindow.POINT / 2.0);
		return trimmedCoordinates;
	}

	private void moveTowardsNextPoint(int[] coordinates, int pointIndex, int targetIndex, double distance) {
		double deltaX = coordinates[targetIndex] - coordinates[pointIndex];
		double deltaY = coordinates[targetIndex + 1] - coordinates[pointIndex + 1];
		double segmentLength = Math.hypot(deltaX, deltaY);
		coordinates[pointIndex] += deltaX / segmentLength * distance;
		coordinates[pointIndex + 1] += deltaY / segmentLength * distance;
	}

	private double length(int[] xy) {
		double total = 0;
		for (int i = 0; i + 3 < xy.length; i += 2) {
			total += Math.hypot(xy[i + 2] - xy[i], xy[i + 3] - xy[i + 1]);
		}
		return total;
	}

	private void arrow(Graphics2D g, int[] xy) {
		int best = 0;
		double bestLen = -1;
		for (int i = 0; i + 3 < xy.length; i += 2) {
			double len = Math.hypot(xy[i + 2] - xy[i], xy[i + 3] - xy[i + 1]);
			if (len > bestLen) {
				bestLen = len;
				best = i;
			}
		}
		double mx = (xy[best] + xy[best + 2]) / 2.0;
		double my = (xy[best + 1] + xy[best + 3]) / 2.0;
		double a = Math.atan2(xy[best + 3] - xy[best + 1], xy[best + 2] - xy[best]);
		int len = 12;
		g.drawLine((int) mx, (int) my, (int) (mx - len * Math.cos(a - 0.6)), (int) (my - len * Math.sin(a - 0.6)));
		g.drawLine((int) mx, (int) my, (int) (mx - len * Math.cos(a + 0.6)), (int) (my - len * Math.sin(a + 0.6)));
	}

	private double[] pointAt(int[] xy, double d) {
		for (int i = 0; i + 3 < xy.length; i += 2) {
			double seg = Math.hypot(xy[i + 2] - xy[i], xy[i + 3] - xy[i + 1]);
			if (d <= seg || i + 4 >= xy.length) {
				double t = seg == 0 ? 0 : Math.min(1, d / seg);
				return new double[] { pathCoordonates[i] + t * (pathCoordonates[i + 2] - pathCoordonates[i]),
						pathCoordonates[i + 1] + t * (pathCoordonates[i + 3] - pathCoordonates[i + 1]) };
			}
			d -= seg;
		}
		return new double[] { xy[0], xy[1] };
	}
}
