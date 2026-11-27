package ro.demo.usersbackend.server.automatic;

import java.util.List;
import java.util.Optional;

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
import ro.demo.usersbackend.common.Queries;
import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.entity.Area;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.LoadUnitType;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.Reservation;
import ro.demo.usersbackend.entity.RetrievalOrder;
import ro.demo.usersbackend.entity.RetrievalOrderLine;
import ro.demo.usersbackend.entity.RetrievalOrderStatus;
import ro.demo.usersbackend.entity.StorageLocation;
import ro.demo.usersbackend.entity.TransportOrder;
import ro.demo.usersbackend.entity.TransportOrderStatus;
import ro.demo.usersbackend.messaging.Broker;
import ro.demo.usersbackend.messaging.MfsMessages;
import ro.demo.usersbackend.messaging.MfsMessages.AckGet;
import ro.demo.usersbackend.messaging.MfsMessages.Get;
import ro.demo.usersbackend.repository.AreaRepository;
import ro.demo.usersbackend.repository.TransportOrderRepository;
import ro.demo.usersbackend.server.mfs.LUCHandler;
import ro.demo.usersbackend.server.mfs.TelegramHandling;
import ro.demo.usersbackend.server.process.AbstractProcess;

public class AutomaticRetrievalService extends AbstractProcess {
	public static final Logger log = LogManager.getLogger("automaticRetrieval");
	private static final Logger mfsLog = LogManager.getLogger("mfsMessages");

	// conexiunea la broker si consumerul de raspunsuri traiesc cat procesul, nu le
	// refacem la fiecare tura
	private final JMSContext context;
	private final JMSConsumer replies;
	private final LUCHandler lucHandler = new LUCHandler();

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
				} else if (Constants.CREATED.equals(node.path("type").asText())) {
					lucHandler.handle(text);
				} else {
					TelegramHandling.getInstance().route(node.path("point").asText(), text);
				}
			} catch (Exception e) {
				log.error("Automatic retrieval: reply not processed {}", e.toString());
			}
		}
	}

	// tote-ul e pe banda; TO-ul ramane ACTIVE pana ajunge la DP1 (il inchide
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

	// to-urile NEW spre DP1: in tranzactie le trecem pe ACTIVE si pregatim
	// GET-ul,
	// dupa commit trimitem GET-ul (in afara tranzactiei)
	private void sendRequests() {
		List<Long> orderIds = new WithSessionAndTransaction<List<Long>>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				setReturnValue(session.createNamedQuery(TransportOrder.FIND_IDS_TO_SEND_TO_CONVEYOR, Long.class)
						.setParameter("status", TransportOrderStatus.NEW)
						.setParameterList("stations", List.of(Constants.DP1))
						.setParameter("decanting", Constants.DECANTING).setParameter("tote", LoadUnitType.TOTE)
						.getResultList());
			}
		}.run();
		log.info("Automatic retrieval: {} orders to send {}", orderIds.size(), orderIds);

		for (

		Long orderId : orderIds) {
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
		new WithSessionAndTransaction<List<RetrievalOrder>>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				Optional<Area> pickingArea = new AreaRepository(session).findByName(Constants.PICKING_AREA);

				if (pickingArea.isEmpty()) {
					log.error("Could not find " + Constants.PICKING_AREA);
					return;
				}

				List<RetrievalOrder> orders = session
						.createNamedQuery(Queries.RETRIEVAL_ORDER_FIND_AUTOMATIC_BY_STATUS_AND_AREA,
								RetrievalOrder.class)
						.setParameter("status", RetrievalOrderStatus.ACTIVE).setParameter("area", pickingArea.get())
						.setParameterList("open", Constants.OPEN_TRANSPORT_STATUSES).getResultList();

				orders: for (RetrievalOrder order : orders) {
					for (RetrievalOrderLine line : order.getLines()) {
						for (Reservation reservation : line.getReservations()) {
							LoadUnit loadUnit = reservation.getStockItem().getLoadUnit();
							Location location = loadUnit.getLocation();
							// doar tote-urile de pe raft (PICK...), nu cele deja pe banda sau in statie
							if (location.getArea() != pickingArea.get() || !(location instanceof StorageLocation)) {
								continue orders;
							}
							if (new TransportOrderRepository(session).findActiveAndNewByLoadUnit(loadUnit).size() > 0) {
								continue orders;
							}
						}
					}

					for (RetrievalOrderLine line : order.getLines()) {
						for (Reservation reservation : line.getReservations()) {
							try {
								LoadUnit loadUnit = reservation.getStockItem().getLoadUnit();
								if (new TransportOrderRepository(session).findActiveAndNewByLoadUnit(loadUnit)
										.size() <= 0) {
									createTransportOrder(session, loadUnit);
								}
							} catch (RuntimeException e) {
								log.error("Automatic retrieval: load unit {} not processed {}",
										reservation.getStockItem().getLoadUnit().getId(), e.toString());
							}
						}
					}
				}
			}
		}.run();
	}

	// tote-ul pleaca din locatia de picking direct spre DP1
	private void createTransportOrder(Session session, LoadUnit loadUnit) {
		Location dp1 = session.createNamedQuery(Location.BY_NAME, Location.class).setParameter("name", Constants.DP1)
				.uniqueResultOptional().orElse(null);
		if (dp1 == null) {
			log.error("Automatic retrieval: location {} is missing", Constants.DP1);
			return;
		}

		session.persist(new TransportOrder(loadUnit.getLocation(), dp1, TransportOrderStatus.NEW, loadUnit));
		log.info("Automatic retrieval: load unit {} goes from {} to {}", loadUnit.getBarcode(),
				loadUnit.getLocation().getName(), Constants.DP1);
	}

//	private Long automaticOrderId(LoadUnit tote) {
//		for (StockItem item : tote.getStockItems()) {
//			if (item.getReservation() != null) {
//				RetrievalOrder order = item.getReservation().getRetrievalOrderLine().getOrder();
//				if (order.isAutomatic()) {
//					return order.getId();
//				}
//			}
//		}
//		return null;
//	}
}