package ro.demo.usersbackend.sim;

public class InfoPoint extends Entity {

	private String lu;

	public InfoPoint(String name) {
		super(name);
	}

	@Override
	public void run() {
		while (true) {

			for (Entity e : getSource()) {
				if (lu == null) {
					String s = e.getAvailableLU();
					if (s != null) {
						lu = s;
						announceLU();
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

	private void announceLU() {
		System.out.println(lu + " arrived at " + name);
	}

	@Override
	public String getAvailableLU() {
		return lu;
	}
}
