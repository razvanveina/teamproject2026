package ro.demo.usersbackend.sim;

public class InfoPoint extends Entity {

	private volatile String lu;

	public InfoPoint(String name) {
		super(name);
	}

	@Override
	public void run() {
		while (true) {

			for (Entity e : getSource()) {
				if (lu == null) {
					String s = e.getAvailableLU(this);
					if (s != null) {
						lu = s;
						announceLU(s);
					}
				}
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void announceLU(String barcode) {
		System.out.println(barcode + " arrived at " + name);
	}

	@Override
	public synchronized String getAvailableLU() {
		String s = lu;
		lu = null;
		return s;
	}
}
