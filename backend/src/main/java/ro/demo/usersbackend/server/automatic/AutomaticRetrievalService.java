package ro.demo.usersbackend.server.automatic;

import java.util.List;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.StorageLocation;
import ro.demo.usersbackend.entity.TransportOrder;
import ro.demo.usersbackend.entity.TransportOrderStatus;
import ro.demo.usersbackend.packagemessaging.Broker;
import ro.demo.usersbackend.packagemessaging.MfsMessages;
import ro.demo.usersbackend.packagemessaging.MfsMessages.AckGet;
import ro.demo.usersbackend.packagemessaging.MfsMessages.Get;
import ro.demo.usersbackend.server.process.AbstractProcess;
import ro.demo.usersbackend.server.session.ServerSessionFactory;

public class AutomaticRetrievalService extends AbstractProcess {
	public static final Logger log = LogManager.getLogger("automaticRetrieval");
	private static final String CONVEYOR = "CONVEYOR";
	private static final List<TransportOrderStatus> OPEN = List.of(TransportOrderStatus.NEW,
			TransportOrderStatus.ACTIVE);

	public AutomaticRetrievalService() {
		super("automaticRetrievalService");
	}

	// @Override
	protected void doAction() {
		try {
			SessionFactory sessionFactory = ServerSessionFactory.getInstance();
			createTransportOrders(sessionFactory);
			try (JMSContext context = new ActiveMQConnectionFactory(Broker.URL).createContext()) {
				sendRequests(sessionFactory, context);
				receiveReplies(sessionFactory, context);
			}
		} catch (Exception e) {
			log.error("Automatic retrieval: {}", e.toString());
		}
	}

	// rasp ACK_GET cand load unitu a ajuns pe conveyor si to e gata
	private void receiveReplies(SessionFactory sessionFactory, JMSContext context) throws JMSException {
		JMSConsumer consumer = context.createConsumer(context.createQueue(MfsMessages.RESPONSE_QUEUE));
		Message message;
		while ((message = consumer.receive(1000)) != null) {
			try {
				String text = ((TextMessage) message).getText();
				AckGet ack = MfsMessages.fromJson(text, AckGet.class);
				sessionFactory.inTransaction(session -> finishTransportOrder(session, ack));
			} catch (Exception e) {
				log.error("Automatic retrieval: reply not processed {}", e.toString());
			}
		}
	}

	private static void finishTransportOrder(Session session, AckGet ack) {
		TransportOrder order = session.find(TransportOrder.class, ack.transportOrderId());
		if (order == null || order.getStatus() != TransportOrderStatus.ACTIVE) {
			log.warn("Automatic retrieval: ACK_GET for order {} ignored", ack.transportOrderId());
			return;
		}

		LoadUnit loadUnit = order.getLoadUnit();
		session.createQuery("from StorageLocation l where l.loadUnit = :loadUnit", StorageLocation.class)
				.setParameter("loadUnit", loadUnit).getResultList().forEach(l -> l.setLoadUnit(null));
		loadUnit.setLocation(order.getTarget());
		order.setStatus(TransportOrderStatus.FINISHED);
		log.info("Automatic retrieval: ACK_GET, load unit {} is on {}", loadUnit.getBarcode(),
				order.getTarget().getName());
	}

	// to-urile new trimise spre conveyor trimit GET si sunt trecute pe active
	private void sendRequests(SessionFactory sessionFactory, JMSContext context) {
		List<Long> orderIds = sessionFactory.fromTransaction(session -> session.createQuery(
				"select t.id from TransportOrder t where t.status = :status and t.target.name = :target order by t.id",
				Long.class).setParameter("status", TransportOrderStatus.NEW).setParameter("target", CONVEYOR)
				.getResultList());

		Queue requests = context.createQueue(MfsMessages.REQUEST_QUEUE);
		for (Long orderId : orderIds) {
			try {
				sessionFactory.inTransaction(session -> {
					TransportOrder order = session.find(TransportOrder.class, orderId);
					Get message = new Get("GET", orderId, order.getLoadUnit().getBarcode(),
							order.getSource().getName());
					context.createProducer().send(requests, MfsMessages.toJson(message));
					order.setStatus(TransportOrderStatus.ACTIVE);
					log.info("Automatic retrieval: sent {}", MfsMessages.toJson(message));
				});
			} catch (Exception e) {
				log.error("Automatic retrieval: order {} not sent {}", orderId, e.toString());
			}
		}
	}

	// load units din hbw rezervate pentru ro automatic fara to deschis
	private void createTransportOrders(SessionFactory sessionFactory) {
		List<Long> loadUnitIds = sessionFactory.fromTransaction(session -> session.createQuery("""
				select distinct u.id from StorageLocation l
				join l.loadUnit u
				join u.stockItems s
				where l.aisle > 0 and s.reservation.retrievalOrderLine.order.automatic = true
				and not exists (select t.id from TransportOrder t where t.loadUnit = u and t.status in :open)
				""", Long.class).setParameterList("open", OPEN).getResultList());

		for (Long loadUnitId : loadUnitIds) {
			try {
				sessionFactory.inTransaction(session -> createTransportOrder(session, loadUnitId));
			} catch (Exception e) {
				log.error("Automatic retrieval: load unit {} not processed {}", loadUnitId, e.toString());
			}
		}
	}

	private static void createTransportOrder(Session session, Long loadUnitId) {
		Location conveyor = session.createQuery("from Location l where l.name = :name", Location.class)
				.setParameter("name", CONVEYOR).uniqueResultOptional().orElse(null);
		if (conveyor == null) {
			log.error("Automatic retrieval: location {} is missing", CONVEYOR);
			return;
		}

		LoadUnit loadUnit = session.find(LoadUnit.class, loadUnitId);
		session.persist(new TransportOrder(loadUnit.getLocation(), conveyor, TransportOrderStatus.NEW, loadUnit));
		log.info("Automatic retrieval: load unit {} goes from {} to {}", loadUnit.getBarcode(),
				loadUnit.getLocation().getName(), CONVEYOR);
	}
}
