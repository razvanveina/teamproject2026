package ro.demo.usersbackend.server.mfs;

import java.util.List;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

import jakarta.jms.JMSContext;
import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.TransportOrder;
import ro.demo.usersbackend.entity.TransportOrderStatus;
import ro.demo.usersbackend.messaging.Broker;
import ro.demo.usersbackend.messaging.MfsMessages;
import ro.demo.usersbackend.messaging.MfsMessages.Arrived;

public abstract class DecisionPointHandler extends PointHandler {
	private static final Logger mfsLog = LogManager.getLogger("mfsMessages");

	private final String point;

	protected DecisionPointHandler(String point) {
		super(point);
		this.point = point;
	}

	// 0 = drumu default, 1 = alternativa
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

		String reply = String.valueOf(decide(arrived.loadUnit(), target));

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
				setReturnValue(session.createNamedQuery("StockItem.countByLoadUnitBarcode", Long.class)
						.setParameter("barcode", barcode).getSingleResult() > 0);
			}
		}.run();
	}

	private String activeTarget(String loadUnit) {
		return new WithSessionAndTransaction<String>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				setReturnValue(session.createNamedQuery("TransportOrder.findActiveTargetName", String.class)
						.setParameter("barcode", loadUnit).setParameter("active", TransportOrderStatus.ACTIVE)
						.setMaxResults(1).uniqueResultOptional().orElse(null));
			}
		}.run();
	}

	// pentru DP-urile din fata unui PTTT (DP4, DP6, DP8): 0 = in zona, 1 = mai
	// departe
	// cu TO: intra doar daca TO-ul e spre PTTT-ul lui, altfel trece (spre OUT sau
	// alt PTTT)
	// fara TO (tote gol): intra daca zona lui e goala; daca are deja un tote, intra
	// doar cand toate zonele de dupa el au cel putin unul si mai e loc, altfel
	// merge mai departe sa umple intai zonele goale
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

				if (inZone >= Constants.PTTT_LANE_SLOTS + 1 || (inZone > 0 && !zonesAfterHaveOne)) {
					setReturnValue(1);
					return;
				}

				LoadUnit tote = session.createNamedQuery(LoadUnit.BY_BARCODE, LoadUnit.class)
						.setParameter("barcode", loadUnit).getSingleResult();
				Location zone = session.createNamedQuery("Location.findByName", Location.class)
						.setParameter("name", pttt).getSingleResult();
				session.persist(new TransportOrder(tote.getLocation(), zone, TransportOrderStatus.ACTIVE, tote));
				setReturnValue(0);
			}
		}.run();
	}

	private long countInZone(Session session, String pttt) {
		return session.createNamedQuery("LoadUnit.countByLocationName", Long.class).setParameter("name", pttt)
				.getSingleResult()
				+ session.createNamedQuery("TransportOrder.countOpenByTargetName", Long.class)
						.setParameter("target", pttt).setParameterList("open", Constants.OPEN_TRANSPORT_STATUSES)
						.getSingleResult();
	}
}