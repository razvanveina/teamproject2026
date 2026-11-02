package ro.demo.usersbackend.init;

import org.hibernate.Session;

import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.TransportOrder;
import ro.demo.usersbackend.entity.TransportOrderStatus;

public class TestTransportOrder {
	private Session session;

	public TestTransportOrder(Session session) {
		super();
		this.session = session;
	}

	public void run() {
		createTransportOrder("1000", "HBW010010101", "HBW020050101");
		createTransportOrder("1001", "HBW010020101", "HBW020060302");
	}

	private void createTransportOrder(String palletBarcode, String sourceName, String destinationName) {
		if (transportOrderExists(palletBarcode)) {
			return;
		}

		Location source = findLocation(sourceName);
		Location destination = findLocation(destinationName);

		LoadUnit loadUnit = findLoadUnit(palletBarcode);

		TransportOrder order = new TransportOrder();
		order.setLoadUnit(loadUnit);
		order.setSource(source);
		order.setTarget(destination);
		order.setStatus(TransportOrderStatus.NEW);
		session.persist(order);
	}

	private boolean transportOrderExists(String barcode) {
		Long count = session
				.createQuery("select count(o) from TransportOrder o where o.loadUnit.barcode = :barcode", Long.class)
				.setParameter("barcode", barcode).getSingleResult();
		return count > 0;
	}

	private Location findLocation(String name) {
		Location location = session.createQuery("from Location l where l.name = :name", Location.class)
				.setParameter("name", name).uniqueResult();
		if (location == null) {
			throw new IllegalStateException("Location " + name + " does not exist");
		}
		return location;
	}

	private LoadUnit findLoadUnit(String barcode) {
		return session.createQuery("from LoadUnit lu where lu.barcode = :barcode", LoadUnit.class)
				.setParameter("barcode", barcode).uniqueResultOptional()
				.orElseThrow(() -> new IllegalStateException("Load unit " + barcode + " does not exist"));
	}

	public static void main(String[] args) {
		new WithSessionAndTransaction<>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				TestTransportOrder tto = new TestTransportOrder(session);
				tto.run();
			}
		}.run();
	}
}
