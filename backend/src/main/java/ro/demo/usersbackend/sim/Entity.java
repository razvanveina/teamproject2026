package ro.demo.usersbackend.sim;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import ro.demo.usersbackend.common.Constants;

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
		List<String> loadUnits = getLoadUnits().stream().map(lu -> "\"" + lu.barcode() + "\"").toList();
		return "{\"id\":" + id + ",\"name\":\"" + name + "\",\"loadUnits\":[" + String.join(",", loadUnits) + "]}";
	}

	public List<SimLoadUnit> getLoadUnits() {
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

	public abstract SimLoadUnit getAvailableLU();

	public SimLoadUnit getAvailableLU(Entity asking) {
		return getAvailableLU();
	}

	public boolean addLoadUnit(SimLoadUnit addedLoadUnit) {
		return false;
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

	private boolean isCarton(SimLoadUnit loadUnit) {
		return Constants.CARTON_TYPE.equals(loadUnit.type());
	}

	protected void tote(Graphics2D g, SimLoadUnit loadUnit, double x, double y) {
		return;
//		String barcode = loadUnit.barcode();
//		int tx = (int) (x - SimWindow.TOTE / 2.0);
//		int ty = (int) (y - SimWindow.TOTE / 2.0);
//		g.setColor(isCarton(loadUnit) ? Constants.CARTON_COLOR : Constants.TOTE_COLOR);
//		g.fillRect(tx, ty, SimWindow.TOTE, SimWindow.TOTE);
//		g.setColor(Color.BLACK);
//		g.setStroke(new BasicStroke(1.75f));
//		g.drawRect(tx, ty, SimWindow.TOTE, SimWindow.TOTE);
//
//		float size = SimWindow.NAME.getSize2D();
//		g.setFont(SimWindow.NAME);
//		while (g.getFontMetrics().stringWidth(barcode) > SimWindow.TOTE - 3 && size > 5f) {
//			size -= 0.5f;
//			g.setFont(SimWindow.NAME.deriveFont(size));
//		}
//		int w = g.getFontMetrics().stringWidth(barcode);
//		g.drawString(barcode, (int) x - w / 2, (int) y + 3);
	}

}
