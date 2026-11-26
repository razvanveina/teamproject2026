package ro.demo.usersbackend.server.mfs;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

import jakarta.jms.Destination;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.entity.ArticlePackagingUnitType;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.StockItem;
import ro.demo.usersbackend.messaging.Broker;
import ro.demo.usersbackend.messaging.MfsMessages;
import ro.demo.usersbackend.messaging.MfsMessages.LoadUnitInfo;
import ro.demo.usersbackend.messaging.MfsMessages.LoadUnitInfoItem;
import ro.demo.usersbackend.repository.LoadUnitRepository;

public final class SimLoadUnitInfoListener {
	private final Logger log = LogManager.getLogger("application");
	private final Logger mfsLog = LogManager.getLogger("mfsMessages");

	private JMSContext context;

	public void start() {
		context = new ActiveMQConnectionFactory(Broker.URL).createContext();
		JMSConsumer consumer = context.createConsumer(context.createQueue(MfsMessages.LOAD_UNIT_INFO_REQUEST_QUEUE));
		consumer.setMessageListener(this::reply);
		log.info("Sim load unit info: listening on {}", MfsMessages.LOAD_UNIT_INFO_REQUEST_QUEUE);
	}

	private void reply(Message request) {
		try {
			Destination replyTo = request.getJMSReplyTo();
			if (replyTo == null || !(request instanceof TextMessage barcodeMessage)) {
				log.warn("Sim load unit info: request without a reply queue or barcode, ignored");
				return;
			}
			String json = MfsMessages.toJson(findLoadUnitInfo(barcodeMessage.getText().trim()));
			context.createProducer().send(replyTo, json);
			mfsLog.info("SERVER -> SIM [load unit info] {}", json);
		} catch (Exception e) {
			log.error("Sim load unit info: reply not sent {}", e.toString());
		}
	}

	private LoadUnitInfo findLoadUnitInfo(String barcode) {
		return new WithSessionAndTransaction<LoadUnitInfo>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				Optional<LoadUnit> foundLoadUnit = new LoadUnitRepository(session).findByBarcode(barcode);
				if (foundLoadUnit.isEmpty()) {
					setReturnValue(new LoadUnitInfo(barcode, null, null, List.of()));
					return;
				}
				LoadUnit loadUnit = foundLoadUnit.get();
				List<LoadUnitInfoItem> items = new ArrayList<>();
				String loadUnitOrderId = null;
				for (StockItem stockItem : loadUnit.getStockItems()) {
					ArticlePackagingUnitType packaging = stockItem.getArticlePackagingUnitType();
					String itemOrderId = reservedOrderId(stockItem);
					if (loadUnitOrderId == null) {
						loadUnitOrderId = itemOrderId;
					}
					items.add(new LoadUnitInfoItem(packaging.getArticle().getDescription(), packaging.getBarcode(),
							stockItem.getQuantity(), packaging.getPackagingUnitType().getQuantity(), itemOrderId));
				}
				setReturnValue(new LoadUnitInfo(barcode, loadUnit.getType().name(), loadUnitOrderId, items));
			}
		}.run();
	}

	private String reservedOrderId(StockItem stockItem) {
		if (stockItem.getReservation() == null) {
			return null;
		}
		return stockItem.getReservation().getRetrievalOrderLine().getOrder().getOrderId();
	}
}
