package ro.demo.usersbackend.server.mfs;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.jms.JMSContext;
import ro.demo.usersbackend.packagemessaging.Broker;
import ro.demo.usersbackend.packagemessaging.MfsMessages;
import ro.demo.usersbackend.packagemessaging.MfsMessages.Arrived;

public abstract class DecisionPointHandler extends PointHandler {
	private static final Logger mfsLog = LogManager.getLogger("mfsMessages");

	private final String point;

	protected DecisionPointHandler(String point) {
		super(point);
		this.point = point;
	}

	// 0 = drumu default, 1 = alternativa
	protected abstract int decide(String loadUnit) throws Exception;

	@Override
	public void handle(String telegram) throws Exception {
		super.handle(telegram);

		Arrived arrived = MfsMessages.fromJson(telegram, Arrived.class);
		String reply = String.valueOf(decide(arrived.loadUnit()));

		try (JMSContext context = new ActiveMQConnectionFactory(Broker.URL).createContext()) {
			context.createProducer().send(context.createQueue(point), reply);
			mfsLog.info("SERVER -> SIM [{}] {}", point, reply);
		}
		log.info("{}: load unit {} reply {}", point, arrived.loadUnit(), reply);
	}
}