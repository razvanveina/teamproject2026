package ro.demo.usersbackend.sim;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.util.Arrays;
import java.util.LinkedList;
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
	private double speedFactor = 1.0;
	private LinkedList<Tote> totes;
	private volatile boolean ready;
	private TimeKeeper timeKeeper = TimeKeeper.getInstance();

	private Point labelPositon;
	private int[] pathCoordonates;
	private boolean isEdited;
	private Shape shape;
	// private Shape anchor;
	private Entity sideSource;
	private Point sideJoin;
	private boolean endsOnBelt;

	public Conveyor(String name, int capacity, Point labelPositon, int... xy) {
		super(name);
		this.totes = new LinkedList<>();
		ready = true;

		this.labelPositon = labelPositon;
		pathCoordonates = Arrays.stream(xy).toArray();
		this.capacity = capacityFromLength();
		if (this.capacity != capacity) {
			log.info("{}: capacity {} from its length, not {}", name, this.capacity, capacity);
		}
		computeTravelTimes();
	}

	private void computeTravelTimes() {
		travelTime = Math.round(length(pathCoordonates) / (Constants.CONVEYOR_SPEED * speedFactor) * 1000);
		slotTravelTime = capacity > 1 ? travelTime / (capacity - 1) : travelTime;
	}

	public long getTravelTime() {
		return travelTime;
	}

	public int getCapacity() {
		return capacity;
	}

	public synchronized void setSpeedFactor(double speedFactor) {
		this.speedFactor = speedFactor;
		computeTravelTimes();
	}

	public synchronized boolean createLU(SimLoadUnit createdLoadUnit) {
		if (!isSlotFree(0, timeKeeper.currentSimulationTime())) {
			return false;
		}
		enterFirstSlot(createdLoadUnit);
		return true;
	}

	public synchronized boolean queueAtEnd(SimLoadUnit queuedLoadUnit) {
		if (totes.size() >= capacity) {
			return false;
		}
		int slotIndex = capacity - 1 - totes.size();
		totes.addLast(
				new Tote(queuedLoadUnit, slotIndex, slotPosition(slotIndex), timeKeeper.currentSimulationTime(), 0));
		return true;
	}

	@Override
	public boolean addLoadUnit(SimLoadUnit addedLoadUnit, Sim sim) {
		if (!super.addLoadUnit(addedLoadUnit, sim)) {
			return false;
		}

		if (!createLU(addedLoadUnit)) {
			return false;
		}

		announceCreated(addedLoadUnit);
		return true;
	}

	private void announceCreated(SimLoadUnit createdLoadUnit) {
		String message = MfsMessages
				.toJson(new Created(Constants.CREATED, name, createdLoadUnit.barcode(), createdLoadUnit.type()));
		try (JMSContext context = new ActiveMQConnectionFactory(Broker.CLIENT_URL).createContext()) {
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
					pullFromSide();
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

	public void joinFromSide(Conveyor source, int x, int y) {
		sideSource = source;
		sideJoin = new Point(x, y);
		source.endsOnBelt = true;
	}

	// tote-ul de la capatul benzii laterale intra in slotul cel mai apropiat de
	// punctul
	// de intalnire, cand slotul e liber
	private void pullFromSide() {
		if (sideSource == null) {
			return;
		}
		int slot;
		synchronized (this) {
			slot = slotNearest(sideJoin);
			if (!isSlotFree(slot, timeKeeper.currentSimulationTime())) {
				return;
			}
		}
		SimLoadUnit joiningLoadUnit = sideSource.getAvailableLU(this);
		if (joiningLoadUnit == null) {
			return;
		}
		synchronized (this) {
			// totes e in ordine de la capatul benzii spre inceput, il punem la locul lui
			int index = 0;
			while (index < totes.size() && totes.get(index).slotIndex > slot) {
				index++;
			}
			totes.add(index,
					new Tote(joiningLoadUnit, slot, slotPosition(slot), timeKeeper.currentSimulationTime(), 0));
		}
	}

	// slotul benzii cel mai apropiat de un punct de pe desen
	private int slotNearest(Point point) {
		double margin = slotMargin();
		double usable = length(pathCoordonates) - 2 * margin;
		int nearest = 0;
		double nearestDistance = Double.MAX_VALUE;
		for (int slot = 0; slot < capacity; slot++) {
			double[] position = pointAt(pathCoordonates, margin + slotPosition(slot) * usable);
			double distance = Math.hypot(position[0] - point.x, position[1] - point.y);
			if (distance < nearestDistance) {
				nearestDistance = distance;
				nearest = slot;
			}
		}
		return nearest;
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
		Point labelPosition = currentLabelPosition(g);
		g.drawString(name, labelPosition.x, labelPosition.y);

//		Conveyor conveyor = (Conveyor) byName.get(name);
		double margin = slotMargin();
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

	public int anchorIndexAt(Point drawingPoint) {
		for (int anchorIndex = 0; anchorIndex < pathCoordonates.length / 2; anchorIndex++) {
			if (anchorShape(anchorIndex).contains(drawingPoint)) {
				return anchorIndex;
			}
		}
		return Constants.NO_ANCHOR;
	}

	public Point getAnchorPosition(int anchorIndex) {
		return new Point(pathCoordonates[anchorIndex * 2], pathCoordonates[anchorIndex * 2 + 1]);
	}

	public void moveAnchor(int anchorIndex, Point newPosition) {
		pathCoordonates[anchorIndex * 2] = newPosition.x;
		pathCoordonates[anchorIndex * 2 + 1] = newPosition.y;
		isEdited = true;
	}

	public boolean isFirstAnchor(int anchorIndex) {
		return anchorIndex == 0;
	}

	public boolean isLastAnchor(int anchorIndex) {
		return anchorIndex == getLastAnchorIndex();
	}

	public int getLastAnchorIndex() {
		return pathCoordonates.length / 2 - 1;
	}

	public int addAnchorAtEnd() {
		int lastAnchorX = pathCoordonates[pathCoordonates.length - 2];
		int lastAnchorY = pathCoordonates[pathCoordonates.length - 1];
		pathCoordonates = Arrays.copyOf(pathCoordonates, pathCoordonates.length + 2);
		pathCoordonates[pathCoordonates.length - 2] = lastAnchorX;
		pathCoordonates[pathCoordonates.length - 1] = lastAnchorY;
		isEdited = true;
		return pathCoordonates.length / 2 - 1;
	}

	public boolean canRemoveAnchor() {
		return pathCoordonates.length / 2 > Constants.MIN_ANCHOR_COUNT;
	}

	public void removeLastAnchor() {
		pathCoordonates = Arrays.copyOf(pathCoordonates, pathCoordonates.length - 2);
		isEdited = true;
	}

	public synchronized void recalculateCapacity() {
		capacity = Math.max(capacityFromLength(), totes.size());
		computeTravelTimes();
		putTotesInSlotsFromEnd();
	}

	// cate cutii incap pe banda: lungimea fara capete, impartita la cutie + spatiu
	private int capacityFromLength() {
		double usableLength = length(pathCoordonates) - 2 * slotMargin();
		int slotGapCount = (int) (usableLength / (SimWindow.TOTE + Constants.TOTE_GAP));
		return Math.max(Constants.MIN_CONVEYOR_CAPACITY, slotGapCount + 1);
	}

	private void putTotesInSlotsFromEnd() {
		long currentTime = timeKeeper.currentSimulationTime();
		int toteIndex = 0;
		for (Tote tote : totes) {
			tote.slotIndex = capacity - 1 - toteIndex;
			tote.startPosition = slotPosition(tote.slotIndex);
			tote.moveStartedAt = currentTime;
			tote.moveDuration = 0;
			toteIndex++;
		}
	}

	public synchronized ConveyorLayout layout() {
		return new ConveyorLayout(name, pathCoordonates.clone(), capacity);
	}

	public synchronized void applyLayout(ConveyorLayout layout) {
		pathCoordonates = layout.path().clone();
		capacity = layout.capacity();
		computeTravelTimes();
		isEdited = true;
	}

	public boolean isEdited() {
		return isEdited;
	}

	private Point currentLabelPosition(Graphics2D g) {
		if (isEdited) {
			return labelPositionNextToLongestSegment(g);
		}
		return labelPositon;
	}

	private Point labelPositionNextToLongestSegment(Graphics2D g) {
		int segmentStart = longestSegmentStart();
		int startX = pathCoordonates[segmentStart];
		int startY = pathCoordonates[segmentStart + 1];
		int endX = pathCoordonates[segmentStart + 2];
		int endY = pathCoordonates[segmentStart + 3];
		int middleX = (startX + endX) / 2;
		int middleY = (startY + endY) / 2;

		FontMetrics fontMetrics = g.getFontMetrics();
		boolean isHorizontalSegment = Math.abs(endX - startX) >= Math.abs(endY - startY);
		if (isHorizontalSegment) {
			int labelWidth = fontMetrics.stringWidth(name);
			return new Point(middleX - labelWidth / 2, middleY - Constants.CONVEYOR_LABEL_OFFSET);
		}
		return new Point(middleX + Constants.CONVEYOR_LABEL_OFFSET, middleY + fontMetrics.getAscent() / 2);
	}

	private int longestSegmentStart() {
		int longestStart = 0;
		double longestLength = 0;
		for (int segmentStart = 0; segmentStart + 3 < pathCoordonates.length; segmentStart += 2) {
			int deltaX = pathCoordonates[segmentStart + 2] - pathCoordonates[segmentStart];
			int deltaY = pathCoordonates[segmentStart + 3] - pathCoordonates[segmentStart + 1];
			double segmentLength = Math.hypot(deltaX, deltaY);
			if (segmentLength > longestLength) {
				longestLength = segmentLength;
				longestStart = segmentStart;
			}
		}
		return longestStart;
	}

	private double slotMargin() {
		return SimWindow.POINT / 2.0 + SimWindow.TOTE / 2.0 + 2;
	}

	private Rectangle anchorShape(int anchorIndex) {
		int halfAnchorSize = Constants.ANCHOR_SIZE / 2;
		int anchorLeft = pathCoordonates[anchorIndex * 2] - halfAnchorSize;
		int anchorTop = pathCoordonates[anchorIndex * 2 + 1] - halfAnchorSize;
		return new Rectangle(anchorLeft, anchorTop, Constants.ANCHOR_SIZE, Constants.ANCHOR_SIZE);
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
		if (!endsOnBelt) {
			moveTowardsNextPoint(trimmedCoordinates, centerLine.length - 2, centerLine.length - 4,
					SimWindow.POINT / 2.0);
		}
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

	@Override
	public synchronized void clear() {
		totes.clear();
	}

	@Override
	public boolean hasLoadUnit(String barcode) {
		for (Tote tote : totes) {
			if (tote.loadUnit.barcode().equals(barcode)) {
				return true;
			}
		}
		return false;
	}
}
