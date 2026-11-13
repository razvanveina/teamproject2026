package ro.demo.usersbackend.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.stereotype.Service;

import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import jakarta.jms.TextMessage;
import ro.demo.usersbackend.packagemessaging.Broker;
import ro.demo.usersbackend.packagemessaging.MfsMessages;

@Service
public class SimulatorService {

	private Queue outQueue;
	private JMSConsumer consumer;
	private JMSContext context;

	public SimulatorService() {
		try {
			context = new ActiveMQConnectionFactory(Broker.URL).createContext();
			consumer = context.createConsumer(context.createQueue(MfsMessages.FRONTEND_RESPONSE_QUEUE));
			outQueue = context.createQueue(MfsMessages.FRONTEND_REQUEST_QUEUE);
		} catch (Exception e) {
			System.out.println("Exception " + e);
		}
	}

	public List<String> getEntities() {
		List<String> entities = new ArrayList<>();

		context.createProducer().send(outQueue, "getEntities");

		try {
			Message message = consumer.receive();
			if (message instanceof TextMessage text) {
				String response = text.getText();
				entities = Arrays.asList(response.split(","));
			}
		} catch (Exception e) {
			System.out.println("Exception " + e);
		}

		return entities;
	}
}
