package ro.demo.usersbackend.server.mfs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

import jakarta.jms.JMSContext;
import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.StockItem;
import ro.demo.usersbackend.entity.TransportOrder;
import ro.demo.usersbackend.entity.TransportOrderStatus;
import ro.demo.usersbackend.messaging.Broker;
import ro.demo.usersbackend.messaging.MfsMessages;
import ro.demo.usersbackend.messaging.MfsMessages.Arrived;

public abstract class DecisionPointHandler extends PointHandler {
	private static final Logger mfsLog = LogManager.getLogger("mfsMessages");

	private final String point;
	private static final int WAIT = -1;
	private final Map<String, Long> waitingSince = new HashMap<>();

	protected DecisionPointHandler(String point) {
		super(point);
		this.point = point;
	}

	// 0 = drumu default, 1 = alternativa, WAIT = asteapta la DP
	protected abstract int decide(String loadUnit, String target) throws Exception;

	@Override
	public void handle(String telegram) throws Exception {
		super.handle(telegram);

		Arrived arrived = MfsMessages.fromJson(telegram, Arrived.class);
		if (!isKnownLoadUnit(arrived.loadUnit())) {
			sendReply(Constants.HOLD);
			log.error("{}: load unit {} does not exist in the backend, held at {}", point, arrived.loadUnit(), point);
			return;
		}

		String target = activeTarget(arrived.loadUnit());
		if (target == null && hasStock(arrived.loadUnit())) {
			sendReply(Constants.HOLD);
			log.error("{}: load unit {} has stock but no active transport order, held at {}", point, arrived.loadUnit(),
					point);
			return;
		}

		int decision = decide(arrived.loadUnit(), target);
		String reply = decision == WAIT ? Constants.HOLD : String.valueOf(decision);

		sendReply(reply);
		log.info("{}: load unit {} going to {}, reply {}", point, arrived.loadUnit(), target, reply);
	}

	private void sendReply(String reply) {
		try (JMSContext context = new ActiveMQConnectionFactory(Broker.URL).createContext()) {
			context.createProducer().send(context.createQueue(point), reply);
			mfsLog.info("SERVER -> SIM [{}] {}", point, reply);
		}
	}

	private boolean isKnownLoadUnit(String barcode) {
		return new WithSessionAndTransaction<Boolean>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				setReturnValue(session.createNamedQuery(LoadUnit.BY_BARCODE, LoadUnit.class)
						.setParameter("barcode", barcode).uniqueResultOptional().isPresent());
			}
		}.run();
	}

	private boolean hasStock(String barcode) {
		return new WithSessionAndTransaction<Boolean>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				setReturnValue(session.createNamedQuery(StockItem.COUNT_BY_LOAD_UNIT_BARCODE, Long.class)
						.setParameter("barcode", barcode).getSingleResult() > 0);
			}
		}.run();
	}

	private String activeTarget(String loadUnit) {
		return new WithSessionAndTransaction<String>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				setReturnValue(session.createNamedQuery(TransportOrder.FIND_ACTIVE_TARGET_NAME, String.class)
						.setParameter("barcode", loadUnit).setParameter("active", TransportOrderStatus.ACTIVE)
						.setMaxResults(1).uniqueResultOptional().orElse(null));
			}
		}.run();
	}

	// pentru DP-urile din fata unui PTTT (DP4, DP6, DP8): 0 = in zona, 1 = mai
	// departe
	// 1. zona lui asteapta target (sursa in PTTS, PTTT gol) -> intra
	// 2. zona lui e goala dar nu asteapta, iar o zona de dupa asteapta -> trece
	// 3. altfel: intra daca zona lui e goala; daca are deja un tote, intra doar
	// cand
	// toate zonele de dupa au cel putin unul si mai e loc
	// cand intra primeste un TO spre PTTT, ca sa fie numarat in zona
	protected int decideForZone(String loadUnit, String target, String pttt) {
		if (target != null) {
			return pttt.equals(target) ? 0 : 1;
		}

		return new WithSessionAndTransaction<Integer>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				long inZone = countInZone(session, pttt);
				List<String> zonesAfter = Constants.PTTT_POINTS.subList(Constants.PTTT_POINTS.indexOf(pttt) + 1,
						Constants.PTTT_POINTS.size());
				boolean zonesAfterHaveOne = zonesAfter.stream().allMatch(next -> countInZone(session, next) > 0);
				boolean zoneAfterWaits = zonesAfter.stream().anyMatch(next -> waitsForTarget(session, next));

				boolean full = inZone >= Constants.PTTT_LANE_SLOTS + 1;
				boolean fillEmptyZonesFirst = inZone > 0 && !zonesAfterHaveOne;
				boolean leaveForWaitingZone = inZone == 0 && !waitsForTarget(session, pttt) && zoneAfterWaits;
				if (full || fillEmptyZonesFirst || leaveForWaitingZone) {
					setReturnValue(1);
					return;
				}

				LoadUnit tote = session.createNamedQuery(LoadUnit.BY_BARCODE, LoadUnit.class)
						.setParameter("barcode", loadUnit).getSingleResult();
				Location zone = session.createNamedQuery(Location.BY_NAME, Location.class)
						.setParameter("name", pttt).getSingleResult();
				session.persist(new TransportOrder(tote.getLocation(), zone, TransportOrderStatus.ACTIVE, tote));
				setReturnValue(0);
			}
		}.run();
	}

	// pentru DP-urile din fata unui PTTS (DP1, DP5, DP7): 0 = in statie, 1 = mai
	// departe, WAIT = asteapta la DP, intra tote-ul cu TO spre PTTS-ul lui, si daca
	// zona e full, asteapta PTTS_WAIT si mai da o tura si incearca din nou atunci
	protected int decideForStation(String loadUnit, String target, String ptts) {
		if (!ptts.equals(target)) {
			return 1;
		}

		boolean room = new WithSessionAndTransaction<Boolean>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				long inStation = session.createNamedQuery(LoadUnit.COUNT_BY_LOCATION_NAME, Long.class)
						.setParameter("name", ptts).getSingleResult();
				long inLane = session.createNamedQuery(TransportOrder.COUNT_IN_LANE_TO_TARGET, Long.class)
						.setParameter("active", TransportOrderStatus.ACTIVE).setParameter("target", ptts)
						.setParameter("point", point).setParameter("barcode", loadUnit).getSingleResult();
				setReturnValue(inStation + inLane < Constants.PTTS_LANE_SLOTS + 1);
			}
		}.run();

		if (room) {
			waitingSince.remove(loadUnit);
			return 0;
		}
		long since = waitingSince.computeIfAbsent(loadUnit, barcode -> System.currentTimeMillis());
		if (System.currentTimeMillis() - since < Constants.PTTS_WAIT) {
			return WAIT;
		}
		waitingSince.remove(loadUnit);
		return 1;
	}

	// zona asteapta target: are sursa in PTTS (sau pe drum spre el) si PTTT-ul e
	// gol
	private boolean waitsForTarget(Session session, String pttt) {
		String ptts = Constants.PTTS_POINTS.get(Constants.PTTT_POINTS.indexOf(pttt));
		return countInZone(session, pttt) == 0 && countInZone(session, ptts) > 0;
	}

	// cate tote-uri are punctul: in el + pe drum spre el (cu TO deschis)
	private long countInZone(Session session, String point) {
		return session.createNamedQuery(LoadUnit.COUNT_BY_LOCATION_NAME, Long.class).setParameter("name", point)
				.getSingleResult()
				+ session.createNamedQuery(TransportOrder.COUNT_OPEN_BY_TARGET_NAME, Long.class)
						.setParameter("target", point).setParameterList("open", Constants.OPEN_TRANSPORT_STATUSES)
						.getSingleResult();
	}
}