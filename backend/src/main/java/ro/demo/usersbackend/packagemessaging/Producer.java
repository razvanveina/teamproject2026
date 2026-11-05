package ro.demo.usersbackend.packagemessaging;

import java.util.Scanner;

import org.apache.activemq.ActiveMQConnectionFactory;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;

public class Producer {

	public static void main(String[] args) throws Exception {

		ConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");

		Scanner scanner = new Scanner(System.in);

		try (JMSContext context = factory.createContext()) {
			Queue queue = context.createQueue("TEST_QUEUE");

			while (true) {
				System.out.print("Mesaj>> ");
				String text = scanner.nextLine();

				if ("exit".equalsIgnoreCase(text)) {
					break;
				}

				context.createProducer().send(queue, text);
				System.out.println("Trimis: " + text);
			}
		}
	}
}
