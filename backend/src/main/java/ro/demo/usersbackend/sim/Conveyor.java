package ro.demo.usersbackend.sim;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Conveyor extends Entity {

	private static final Logger log = LogManager.getLogger("application");

	public record ToteOnBelt(SimLoadUnit loadUnit, double progress) {
	}

	private static final class Tote {
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

	public Conveyor(String name, long travelTime, int capacity) {
		super(name);
		this.travelTime = travelTime;
		this.capacity = capacity;
		this.slotTravelTime = capacity > 1 ? travelTime / (capacity - 1) : travelTime;
		this.totes = new ArrayDeque<>();
		ready = true;
	}

	public long getTravelTime() {
		return travelTime;
	}

	public int getCapacity() {
		return capacity;
	}

	public synchronized void createLU(SimLoadUnit createdLoadUnit) {
		if (isSlotFree(0, timeKeeper.currentSimulationTime())) {
			enterFirstSlot(createdLoadUnit);
		}
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
}
