package ro.demo.usersbackend.sim;

public class TargetTransportPoint extends ServerPoint {

	public TargetTransportPoint(String name) {
		super(name);
	}

	@Override
	protected void step() throws InterruptedException {
		for (Entity e : getSource()) {
			if (barcode == null) {
				barcode = e.getAvailableLU(this);
			}
		}

		if (barcode != null) {
			put("Ready" + barcode);
			barcode = null;
		}

		Thread.sleep(100);
	}

	@Override
	public String getAvailableLU() {
		return null;
	}

	@Override
	public String toJSON(int id) {
		StringBuilder builder = new StringBuilder();

		builder.append("{" + super.toJSON(id) + ",");
		builder.append("\"barcodes\":[");

		builder.append("\"" + super.barcode + "\"");

		builder.append("]}");
		return builder.toString();
	}
}
