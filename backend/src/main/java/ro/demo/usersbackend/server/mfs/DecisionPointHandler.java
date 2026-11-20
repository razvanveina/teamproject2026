package ro.demo.usersbackend.server.mfs;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.jms.JMSContext;
import ro.demo.usersbackend.entity.TransportOrderStatus;
import ro.demo.usersbackend.messaging.Broker;
import ro.demo.usersbackend.messaging.MfsMessages;
import ro.demo.usersbackend.messaging.MfsMessages.Arrived;
import ro.demo.usersbackend.server.session.ServerSessionFactory;

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
		String target = activeTarget(arrived.loadUnit());
		String reply = String.valueOf(decide(arrived.loadUnit(), target));

		try (JMSContext context = new ActiveMQConnectionFactory(Broker.URL).createContext()) {
			context.createProducer().send(context.createQueue(point), reply);
			mfsLog.info("SERVER -> SIM [{}] {}", point, reply);
		}
		log.info("{}: load unit {} going to {}, reply {}", point, arrived.loadUnit(), target, reply);
	}

	private String activeTarget(String loadUnit) throws Exception {
		return ServerSessionFactory.getInstance().fromTransaction(
				session -> session.createNamedQuery("TransportOrder.findActiveTargetName", String.class)
						.setParameter("barcode", loadUnit).setParameter("active", TransportOrderStatus.ACTIVE)
						.setMaxResults(1).uniqueResultOptional().orElse(null));
	}
}