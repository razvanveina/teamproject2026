package ro.demo.usersbackend.server.mfs;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

import jakarta.jms.Destination;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.Message;
import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.messaging.Broker;
import ro.demo.usersbackend.messaging.MfsMessages;
import ro.demo.usersbackend.messaging.MfsMessages.SetupLoadUnit;
import ro.demo.usersbackend.repository.LoadUnitRepository;

// raspunde la butonul Setup din simulator cu load unit-urile care stau pe punctele de banda in DB
public final class SimSetupListener {
	public static final Logger log = LogManager.getLogger("application");
	private static final Logger mfsLog = LogManager.getLogger("mfsMessages");

	// punctele de banda plus OUT; gate-urile nu, cartonul de acolo a iesit deja
	private static final List<String> SETUP_POINTS = Stream
			.concat(Constants.CONVEYOR_POINTS.stream().filter(point -> !point.startsWith("GP")),
					Stream.of(Constants.OUT))
			.toList();

	private JMSContext context;

	// listener JMS, nu proces: raspunde imediat, nu la urmatorul ciclu de
	// PROCESS_INTERVAL
	public void start() {
		context = new ActiveMQConnectionFactory(Broker.URL).createContext();
		JMSConsumer consumer = context.createConsumer(context.createQueue(MfsMessages.SETUP_REQUEST_QUEUE));
		consumer.setMessageListener(this::reply);
		log.info("Sim setup: listening on {}", MfsMessages.SETUP_REQUEST_QUEUE);
	}

	private void reply(Message request) {
		try {
			Destination replyTo = request.getJMSReplyTo();
			if (replyTo == null) {
				log.warn("Sim setup: request without a reply queue, ignored");
				return;
			}
			String json = MfsMessages.toJson(findLoadUnitsOnPoints());
			context.createProducer().send(replyTo, json);
			mfsLog.info("SERVER -> SIM [setup] {}", json);
		} catch (Exception e) {
			log.error("Sim setup: reply not sent {}", e.toString());
		}
	}

	private List<SetupLoadUnit> findLoadUnitsOnPoints() {
		return new WithSessionAndTransaction<List<SetupLoadUnit>>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				LoadUnitRepository loadUnitRepository = new LoadUnitRepository(session);
				List<SetupLoadUnit> loadUnits = new ArrayList<>();
				for (String point : SETUP_POINTS) {
					for (LoadUnit loadUnit : loadUnitRepository.findByLocationName(point)) {
						loadUnits.add(new SetupLoadUnit(point, loadUnit.getBarcode(), loadUnit.getType().name()));
					}
				}
				setReturnValue(loadUnits);
			}
		}.run();
	}
}