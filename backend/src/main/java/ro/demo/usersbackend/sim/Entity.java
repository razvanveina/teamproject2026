package ro.demo.usersbackend.sim;

import java.util.ArrayList;
import java.util.List;

public abstract class Entity implements Runnable {
	protected String name;
	private List<Entity> source = new ArrayList<>();
	private List<Entity> target = new ArrayList<>();

	public Entity(String name) {
		this.setName(name);
		new Thread(this).start();
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
			for (Entity e : source) {

			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
