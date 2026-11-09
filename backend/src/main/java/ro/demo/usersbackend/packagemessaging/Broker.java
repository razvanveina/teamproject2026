package ro.demo.usersbackend.packagemessaging;

import org.apache.activemq.broker.BrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// broker-ul ActiveMQ pornit odata cu serveru
public class Broker {
	public static final String URL = "tcp://localhost:61616";

	public static final Logger log = LogManager.getLogger("application");
	private static BrokerService broker;

	public static synchronized void start() throws Exception {
		broker = new BrokerService();
		broker.setBrokerName("broker");
		broker.setPersistent(false);
		broker.setUseJmx(false);
		broker.addConnector(URL);
		broker.start();
		log.info("Message broker started on {}", URL);
	}

	public static synchronized void stop() throws Exception {
		if (broker != null) {
			broker.stop();
			log.info("Message broker stopped");
		}
	}
}