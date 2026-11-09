package ro.demo.usersbackend.packagemessaging;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import jakarta.jms.TextMessage;

// citeste mesajele din TEST_QUEUE intr-un thread, pornit cu serverul dupa broker
public class Consumer {
	private static final Logger log = LogManager.getLogger("application");

	public static void start() {
		Thread thread = new Thread(Consumer::listen, "messageConsumer");
		thread.setDaemon(true);
		thread.start();
	}

	private static void listen() {
		ConnectionFactory factory = new ActiveMQConnectionFactory(Broker.URL);

		try (JMSContext context = factory.createContext()) {
			Queue queue = context.createQueue("TEST_QUEUE");
			JMSConsumer consumer = context.createConsumer(queue);
			log.info("Consumer is waiting for messages on TEST_QUEUE");

			while (true) {
				Message message = consumer.receive();
				if (message instanceof TextMessage text) {
					log.info("Consumer received: {}", text.getText());
				}
			}
		} catch (Exception e) {
			log.error("Consumer stopped: {}", e.toString());
		}
	}
}