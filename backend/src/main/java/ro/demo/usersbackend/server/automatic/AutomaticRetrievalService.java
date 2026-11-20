package ro.demo.usersbackend.server.automatic;

import java.util.List;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.LoadUnitType;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.TransportOrder;
import ro.demo.usersbackend.entity.TransportOrderStatus;
import ro.demo.usersbackend.messaging.Broker;
import ro.demo.usersbackend.messaging.MfsMessages;
import ro.demo.usersbackend.messaging.MfsMessages.AckGet;
import ro.demo.usersbackend.messaging.MfsMessages.Get;
import ro.demo.usersbackend.server.mfs.TelegramHandling;
import ro.demo.usersbackend.server.process.AbstractProcess;

public class AutomaticRetrievalService extends AbstractProcess {
	public static final Logger log = LogManager.getLogger("automaticRetrieval");
	private static final Logger mfsLog = LogManager.getLogger("mfsMessages");

	// conexiunea la broker si consumerul de raspunsuri traiesc cat procesul, nu le
	// refacem la fiecare tura
	private final JMSContext context;
	private final JMSConsumer replies;

	public AutomaticRetrievalService() {
		super("automaticRetrievalService");
		context = new ActiveMQConnectionFactory(Broker.URL).createContext();
		replies = context.createConsumer(context.createQueue(MfsMessages.RESPONSE_QUEUE));
	}

	@Override
	protected void doAction() {
		try {
			createTransportOrders();
			sendRequests();
			receiveReplies();
		} catch (Exception e) {
			log.error("Automatic retrieval: {}", e.toString());
		}
	}

	// ACK_GET confirma ca TPS1 a luat tote-ul, restul telegramelor (ARRIVED) merg
	// la handlerul punctului
	private void receiveReplies() throws JMSException {
		Message message;
		while ((message = replies.receive(Constants.ACK_GET_WAIT)) != null) {
			try {
				String text = ((TextMessage) message).getText();
				mfsLog.info("SIM -> SERVER [{}] {}", MfsMessages.RESPONSE_QUEUE, text);
				JsonNode node = MfsMessages.fromJson(text, JsonNode.class);
				if ("ACK_GET".equals(node.path("type").asText())) {
					AckGet ack = MfsMessages.fromJson(text, AckGet.class);
					new WithSessionAndTransaction<>() {
						@Override
						protected void executeBusinessLogic(Session session) {
							confirmGet(session, ack);
						}
					}.run();
				} else {
					TelegramHandling.getInstance().route(node.path("point").asText(), text);
				}
			} catch (Exception e) {
				log.error("Automatic retrieval: reply not processed {}", e.toString());
			}
		}
	}

	// tote-ul e pe banda; TO-ul ramane ACTIVE pana ajunge la PTTS1 (il inchide
	// PointHandler)
	private void confirmGet(Session session, AckGet ack) {
		TransportOrder order = session.find(TransportOrder.class, ack.transportOrderId());
		if (order == null || order.getStatus() != TransportOrderStatus.ACTIVE) {
			log.warn("Automatic retrieval: ACK_GET for order {} ignored", ack.transportOrderId());
			return;
		}
		log.info("Automatic retrieval: ACK_GET, load unit {} is on the conveyor, going to {}",
				order.getLoadUnit().getBarcode(), order.getTarget().getName());
	}

	// to-urile NEW spre PTTS1: in tranzactie le trecem pe ACTIVE si pregatim
	// GET-ul,
	// dupa commit trimitem GET-ul (in afara tranzactiei)
	private void sendRequests() {
		List<Long> orderIds = new WithSessionAndTransaction<List<Long>>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				setReturnValue(session.createNamedQuery("TransportOrder.findIdsByStatusAndTargetName", Long.class)
						.setParameter("status", TransportOrderStatus.NEW).setParameter("target", Constants.PTTS1)
						.getResultList());
			}
		}.run();

		for (Long orderId : orderIds) {
			try {
				Get message = new WithSessionAndTransaction<Get>() {
					@Override
					protected void executeBusinessLogic(Session session) {
						TransportOrder order = session.find(TransportOrder.class, orderId);
						order.setStatus(TransportOrderStatus.ACTIVE);
						setReturnValue(new Get("GET", orderId, order.getLoadUnit().getBarcode(),
								order.getLoadUnit().getType().toString(), order.getSource().getName()));
					}
				}.run();

				// de la decanting tote-ul intra pe banda prin TPS2, restul prin TPS1
				String queueName = MfsMessages.REQUEST_QUEUE;
				if (Constants.DECANTING.equals(message.location())) {
					queueName = MfsMessages.DECANTING_REQUEST_QUEUE;
				}
				context.createProducer().send(context.createQueue(queueName), MfsMessages.toJson(message));
				mfsLog.info("SERVER -> SIM [{}] {}", queueName, MfsMessages.toJson(message));
				log.info("Automatic retrieval: sent {}", MfsMessages.toJson(message));
			} catch (RuntimeException e) {
				log.error("Automatic retrieval: order {} not sent {}", orderId, e.toString());
			}
		}
	}

	// totes din PICKING cu stoc rezervat pentru ro automat, fara to deschis
	private void createTransportOrders() {
		List<Long> loadUnitIds = new WithSessionAndTransaction<List<Long>>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				setReturnValue(session.createNamedQuery("LoadUnit.findPickingTotesForAutomaticOrders", Long.class)
						.setParameter("tote", LoadUnitType.TOTE).setParameter("pick", Constants.PICK_PREFIX + "%")
						.setParameterList("open", Constants.OPEN_TRANSPORT_STATUSES).getResultList());
			}
		}.run();

		for (Long loadUnitId : loadUnitIds) {
			try {
				new WithSessionAndTransaction<>() {
					@Override
					protected void executeBusinessLogic(Session session) {
						createTransportOrder(session, loadUnitId);
					}
				}.run();
			} catch (RuntimeException e) {
				log.error("Automatic retrieval: load unit {} not processed {}", loadUnitId, e.toString());
			}
		}
	}

	// tote-ul pleaca din locatia de picking direct spre statia PTTS1
	private void createTransportOrder(Session session, Long loadUnitId) {
		Location station = session.createNamedQuery("Location.findByName", Location.class)
				.setParameter("name", Constants.PTTS1).uniqueResultOptional().orElse(null);
		if (station == null) {
			log.error("Automatic retrieval: location {} is missing", Constants.PTTS1);
			return;
		}

		LoadUnit loadUnit = session.find(LoadUnit.class, loadUnitId);
		session.persist(new TransportOrder(loadUnit.getLocation(), station, TransportOrderStatus.NEW, loadUnit));
		log.info("Automatic retrieval: load unit {} goes from {} to {}", loadUnit.getBarcode(),
				loadUnit.getLocation().getName(), Constants.PTTS1);
	}
}