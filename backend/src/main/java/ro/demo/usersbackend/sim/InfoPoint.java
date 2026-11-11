package ro.demo.usersbackend.sim;

public class InfoPoint extends ServerPoint {

	private volatile String lu;

	public InfoPoint(String name) {
		super(name);
	}

	@Override
	protected void step() throws Exception {
		for (Entity e : getSource()) {
			if (lu == null) {
				String s = e.getAvailableLU(this);
				if (s != null) {
					lu = s;
					announceLU(s);
				}
			}
		}
		Thread.sleep(1000);
	}

	@Override
	public synchronized String getAvailableLU() {
		String s = lu;
		lu = null;
		return s;
	}
}