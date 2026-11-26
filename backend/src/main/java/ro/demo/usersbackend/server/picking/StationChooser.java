package ro.demo.usersbackend.server.picking;

import org.hibernate.Session;

import ro.demo.usersbackend.common.Constants;

public final class StationChooser {
	private StationChooser() {
	}

	public static String sourceStationFor(Session session, Long orderId, String ignore) {
		if (orderId != null) {
			for (int i = 0; i < Constants.PTTS_POINTS.size(); i++) {
				String ptts = Constants.PTTS_POINTS.get(i);
				String pttt = Constants.PTTT_POINTS.get(i);
				long busy = orderItemsOn(session, ptts, orderId, ignore) + orderItemsOn(session, pttt, orderId, ignore)
						+ openOrdersFor(session, ptts, orderId);
				if (busy > 0) {
					return ptts;
				}
			}
		}

		// statiile unde PTTT nu are deja un alt order la care se lucreaza si alegem
		// care e cea mai putin incarcata statie
		String best = Constants.PTTS_POINTS.get(0);
		boolean bestFree = false;
		long bestCount = Long.MAX_VALUE;
		for (int i = 0; i < Constants.PTTS_POINTS.size(); i++) {
			String ptts = Constants.PTTS_POINTS.get(i);
			String pttt = Constants.PTTT_POINTS.get(i);
			boolean free = session.createNamedQuery("StockItem.countOnLocation", Long.class)
					.setParameter("location", pttt).getSingleResult() == 0;
			long count = openOrdersTo(session, ptts) + totesOn(session, ptts);
			if ((free && !bestFree) || (free == bestFree && count < bestCount)) {
				best = ptts;
				bestFree = free;
				bestCount = count;
			}
		}
		return best;
	}

	public static String freeTargetStation(Session session) {
		for (String pttt : Constants.PTTT_POINTS) {
			if (isTargetFree(session, pttt)) {
				return pttt;
			}
		}
		return null;
	}

	public static boolean isTargetFree(Session session, String pttt) {
		long onIt = totesOn(session, pttt);
		return onIt == 0 && openOrdersTo(session, pttt) == 0;
	}

	private static long openOrdersTo(Session session, String target) {
		return session.createNamedQuery("TransportOrder.countOpenByTargetName", Long.class)
				.setParameter("target", target).setParameterList("open", Constants.OPEN_TRANSPORT_STATUSES)
				.getSingleResult();
	}

	private static long totesOn(Session session, String location) {
		return session.createNamedQuery("LoadUnit.countByLocationName", Long.class).setParameter("name", location)
				.getSingleResult();
	}

	private static long openOrdersFor(Session session, String target, Long orderId) {
		return session.createNamedQuery("TransportOrder.countOpenToTargetForOrder", Long.class)
				.setParameter("target", target).setParameterList("open", Constants.OPEN_TRANSPORT_STATUSES)
				.setParameter("orderId", orderId).getSingleResult();
	}

	private static long orderItemsOn(Session session, String location, Long orderId, String ignore) {
		return session.createNamedQuery("StockItem.countForOrderOnLocation", Long.class)
				.setParameter("location", location).setParameter("orderId", orderId).setParameter("ignore", ignore)
				.getSingleResult();
	}
}