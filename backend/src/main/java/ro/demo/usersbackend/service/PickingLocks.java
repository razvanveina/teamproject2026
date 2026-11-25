package ro.demo.usersbackend.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import ro.demo.usersbackend.common.Constants;

@Component
public class PickingLocks {
	private record Holder(String user, Instant lastSeen) {
	}

	private final Map<Integer, Holder> zones = new HashMap<>();

	public synchronized String occupyZone(int zone, String user) {
		Instant now = Instant.now();
		Holder holder = zones.get(zone);
		if (holder != null && !holder.user().equals(user)
				&& holder.lastSeen().plus(Constants.SIM_PICKING_LOCK).isAfter(now)) {
			return holder.user();
		}
		zones.entrySet().removeIf(entry -> entry.getKey() != zone && entry.getValue().user().equals(user));
		zones.put(zone, new Holder(user, now));
		return null;
	}

	public synchronized void leaveZone(int zone, String user) {
		Holder holder = zones.get(zone);
		if (holder != null && holder.user().equals(user)) {
			zones.remove(zone);
		}
	}
}
