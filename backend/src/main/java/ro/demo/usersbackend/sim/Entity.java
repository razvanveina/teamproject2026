package ro.demo.usersbackend.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class Entity implements Runnable {
	static final ReadWriteLock LOCK = new ReentrantReadWriteLock(true);

	protected String name;
	private List<Entity> source = new ArrayList<>();
	private List<Entity> target = new ArrayList<>();

	public Entity(String name) {
		this.setName(name);
	}

	public void start() {
		new Thread(this, name).start();
	}

	public EntityState snapshot() {
		return new EntityState(name, new ArrayList<>(getLoadUnits()), null, false);
	}

	public void restore(EntityState state) {
	}

	public String toJSON(int id) {
		List<String> loadUnits = getLoadUnits().stream().map(lu -> "\"" + lu + "\"").toList();
		return "{\"id\":" + id + ",\"name\":\"" + name + "\",\"loadUnits\":[" + String.join(",", loadUnits) + "]}";
	}

	public List<String> getLoadUnits() {
		return List.of();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Entity> getSource() {
		return source;
	}

	public void setSource(List<Entity> source) {
		this.source = source;
	}

	public List<Entity> getTarget() {
		return target;
	}

	public void setTarget(List<Entity> target) {
		this.target = target;
	}

	public abstract String getAvailableLU();

	public String getAvailableLU(Entity asking) {
		return getAvailableLU();
	}

	@Override
	public void run() {
		while (true) {
//			for (Entity e : source) {
//
//			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
