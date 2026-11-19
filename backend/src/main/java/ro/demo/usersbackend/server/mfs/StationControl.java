package ro.demo.usersbackend.server.mfs;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.jms.JMSContext;
import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.messaging.Broker;

public final class StationControl {
	public static final Logger log = LogManager.getLogger("application");
	private static final Logger mfsLog = LogManager.getLogger("mfsMessages");

	private static StationControl instance;

	private StationControl() {
	}

	public static StationControl getInstance() {
		if (instance == null) {
			instance = new StationControl();
		}
		return instance;
	}

	// pickingu e gata, eliberam lu din statie (PTTS1 sau PTTT1)
	public void release(String station) throws Exception {
		try (JMSContext context = new ActiveMQConnectionFactory(Broker.URL).createContext()) {
			context.createProducer().send(context.createQueue(station), Constants.RELEASE);
			mfsLog.info("SERVER -> SIM [{}] {}", station, Constants.RELEASE);
		}
		log.info("{}: released", station);
	}
}