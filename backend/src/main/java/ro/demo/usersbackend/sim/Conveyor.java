package ro.demo.usersbackend.sim;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Conveyor extends Entity implements Runnable {

	Map<String, Long> lus = new HashMap<>();

	public Conveyor(String name) {
		super(name);
	}

	public void createLU(String barcode) {
		lus.put(barcode, System.currentTimeMillis());
	}

	public String getAvailableLU() {
		String keep = null;
		for (Entry<String, Long> e : lus.entrySet()) {
			if (e.getValue() < System.currentTimeMillis() - 5000) {
				keep = e.getKey();
			}
		}

		lus.remove(keep);

		return keep;
	}

}
