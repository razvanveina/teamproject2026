package ro.demo.usersbackend.packagemessaging;

import org.apache.activemq.ActiveMQConnectionFactory;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import jakarta.jms.TextMessage;

public class Consumer {

	public static void main(String[] args) throws Exception {

		ConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");

		try (JMSContext context = factory.createContext()) {

			Queue queue = context.createQueue("TEST_QUEUE");

			JMSConsumer consumer = context.createConsumer(queue);

			System.out.println("Astept mesaj...");

			while (true) {
				TextMessage message = (TextMessage) consumer.receive();

				System.out.println("Primit: " + message.getText());
			}
		}
	}
}