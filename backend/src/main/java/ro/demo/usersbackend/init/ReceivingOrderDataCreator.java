package ro.demo.usersbackend.init;

import org.hibernate.Session;

import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.entity.Article;
import ro.demo.usersbackend.entity.ReceivingOrder;
import ro.demo.usersbackend.entity.ReceivingOrderLine;
import ro.demo.usersbackend.entity.ReceivingOrderLineStatus;
import ro.demo.usersbackend.entity.ReceivingOrderStatus;

public class ReceivingOrderDataCreator {
	private Session session;

	public ReceivingOrderDataCreator(Session session) {
		super();
		this.session = session;
	}

	public void run() {
		createReceivingOrders();
	}

	private void createReceivingOrders() {

//		session.createQuery("from Employee where id = :id", Employee.class);

		Article art1 = session.find(Article.class, 1);
		Article art2 = session.find(Article.class, 2);

		ReceivingOrder order = new ReceivingOrder("123", ReceivingOrderStatus.NEW);

		ReceivingOrderLine line1 = new ReceivingOrderLine(art1, 10, 10, order, ReceivingOrderLineStatus.FINISHED);
		ReceivingOrderLine line2 = new ReceivingOrderLine(art2, 15, 0, order, ReceivingOrderLineStatus.NEW);
		session.persist(order);
		session.persist(line1);
		session.persist(line2);
	}

	public static void main(String[] args) {
		new WithSessionAndTransaction<>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				ReceivingOrderDataCreator idc = new ReceivingOrderDataCreator(session);
				idc.run();
			}
		}.run();

	}
}
