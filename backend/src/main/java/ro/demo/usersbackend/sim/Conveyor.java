package ro.demo.usersbackend.sim;

import java.util.ArrayDeque;
import java.util.Deque;

public class Conveyor extends Entity {

	private record Tote(String barcode, long enteredAt) {
	}

	private long travelTime;
	private int capacity;
	private Deque<Tote> totes;
	private long lastEnteredAt;
	private volatile boolean ready;

	public Conveyor(String name, long travelTime, int capacity) {
		super(name);
		this.travelTime = travelTime;
		this.capacity = capacity;
		this.totes = new ArrayDeque<>();
		ready = true;
	}

	public synchronized void createLU(String barcode) {
		if (totes.size() < capacity) {
			add(barcode);
		}
	}

	@Override
	public void run() {
		try {
			while (!ready) {
				Thread.sleep(10);
			}

			while (true) {
				pull();
				Thread.sleep(100);
			}
		} catch (InterruptedException e) {
			return;
		}
	}

	private void pull() {
		synchronized (this) {
			if (totes.size() >= capacity || System.currentTimeMillis() - lastEnteredAt < travelTime / capacity) {
				return;
			}
		}

		for (Entity e : getSource()) {
			String barcode = e.getAvailableLU(this);
			if (barcode != null) {
				synchronized (this) {
					add(barcode);
				}
				return;
			}
		}
	}

	private void add(String barcode) {
		lastEnteredAt = System.currentTimeMillis();
		totes.addLast(new Tote(barcode, lastEnteredAt));
	}

	@Override
	public synchronized String getAvailableLU() {
		Tote first = totes.peekFirst();
		if (first == null || System.currentTimeMillis() - first.enteredAt() < travelTime) {
			return null;
		}
		totes.pollFirst();
		return first.barcode();
	}

}
