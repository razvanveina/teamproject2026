package ro.demo.usersbackend.server.mfs;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.jms.JMSContext;
import ro.demo.usersbackend.packagemessaging.Broker;

public final class StationControl {
	public static final Logger log = LogManager.getLogger("application");

	private StationControl() {
	}

	// pickingu e gata, eliberam lu din statie (PTTS1 sau PTTT1)
	public static void release(String station) throws Exception {
		try (JMSContext context = new ActiveMQConnectionFactory(Broker.URL).createContext()) {
			context.createProducer().send(context.createQueue(station), "release");
		}
		log.info("{}: released", station);
	}
}