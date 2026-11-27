package ro.demo.usersbackend.server.picking;

import org.hibernate.Session;

import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.common.Queries;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.StockItem;
import ro.demo.usersbackend.entity.TransportOrder;

public class StationChooser {

	public String sourceStationFor(Session session, Long orderId, String ignore) {
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
			boolean free = session.createNamedQuery(StockItem.COUNT_ON_LOCATION, Long.class)
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

	public String freeTargetStation(Session session) {
		for (String pttt : Constants.PTTT_POINTS) {
			if (isTargetFree(session, pttt)) {
				return pttt;
			}
		}
		return null;
	}

	public boolean isTargetFree(Session session, String pttt) {
		long onIt = totesOn(session, pttt);
		return onIt == 0 && openOrdersTo(session, pttt) == 0;
	}

	private long openOrdersTo(Session session, String target) {
		return session.createNamedQuery(TransportOrder.COUNT_OPEN_BY_TARGET_NAME, Long.class)
				.setParameter("target", target).setParameterList("open", Constants.OPEN_TRANSPORT_STATUSES)
				.getSingleResult();
	}

	private long totesOn(Session session, String location) {
		return session.createNamedQuery(LoadUnit.COUNT_BY_LOCATION_NAME, Long.class).setParameter("name", location)
				.getSingleResult();
	}

	private long openOrdersFor(Session session, String target, Long orderId) {
		return session.createNamedQuery(Queries.COUNT_OPEN_TO_TARGET_FOR_ORDER, Long.class)
				.setParameter("target", target).setParameterList("open", Constants.OPEN_TRANSPORT_STATUSES)
				.setParameter("orderId", orderId).getSingleResult();
	}

	private long orderItemsOn(Session session, String location, Long orderId, String ignore) {
		return session.createNamedQuery(StockItem.COUNT_FOR_ORDER_ON_LOCATION, Long.class)
				.setParameter("location", location).setParameter("orderId", orderId).setParameter("ignore", ignore)
				.getSingleResult();
	}
}