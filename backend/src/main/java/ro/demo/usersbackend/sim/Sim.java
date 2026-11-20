package ro.demo.usersbackend.sim;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import jakarta.jms.TextMessage;
import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.messaging.Broker;
import ro.demo.usersbackend.messaging.MfsMessages;

public class Sim {

	private static final Path SNAPSHOT = Path.of("sim-snapshot.bin");
	private static final Path SNAPSHOT_TMP = Path.of("sim-snapshot.bin.tmp");

	private final Logger log = LogManager.getLogger("application");
	private final List<Entity> entities = new ArrayList<>();

	private Queue outQueue;
	private JMSConsumer consumer;
	private JMSContext context;

	public static void main(String[] args) throws IOException {
		Sim sim = new Sim();
		sim.run();
		SimWindow window = new SimWindow(sim.getEntities());
		window.open(sim::save);

		sim.initComms();
		new Thread(sim::listen).start();

		new CustomHttpServer(sim);

		Runtime.getRuntime().addShutdownHook(new Thread(sim::save));
		Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(sim::save, 5, 5, TimeUnit.SECONDS);
		new Thread(() -> {
			Scanner in = new Scanner(System.in);
			try {
				while (in.hasNextLine()) {
					if (in.nextLine().trim().equalsIgnoreCase("stop")) {
						System.exit(0);
					}
				}
			} finally {
				in.close();
			}
		}).start();
	}

	public void initComms() {
		try {
			context = new ActiveMQConnectionFactory(Broker.URL).createContext();
			consumer = context.createConsumer(context.createQueue(MfsMessages.FRONTEND_REQUEST_QUEUE));
			outQueue = context.createQueue(MfsMessages.FRONTEND_RESPONSE_QUEUE);

		} catch (Exception e) {
			log.error("Exception", e);
		}
	}

	private void listen() {
		while (true) {
			try {
				Message message = consumer.receive();
				if (message instanceof TextMessage text) {
					String request = text.getText();
					if (request.equals(Constants.GET_ENTITIES)) {
						String response = entities.stream().map(Entity::getName).collect(Collectors.joining(","));
						put(response);
					} else if (request.startsWith(Constants.CREATE_LU)) {
						String[] s = request.split(" ");
						entities.forEach(e -> {
							if (e.getName().equals(s[2]) && e.getClass() == Conveyor.class) {
								((Conveyor) e)
										.createLU(new SimLoadUnit(s[1], s.length > 3 ? s[3] : Constants.TOTE_TYPE));
							}
						});
					}
				}
			} catch (Exception e) {
				log.error("Exception", e);
			}
		}
	}

	private void put(String message) {
		context.createProducer().send(outQueue, message);
		log.info(message);
	}

	private synchronized void save() {
		Lock lock = Entity.LOCK.writeLock();
		try {
			if (!lock.tryLock(2, TimeUnit.SECONDS)) {
				return;
			}
			List<EntityState> states;
			try {
				states = entities.stream().map(Entity::snapshot).filter(s -> !s.loadUnits().isEmpty()).toList();
			} finally {
				lock.unlock();
			}
			try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(SNAPSHOT_TMP))) {
				out.writeObject(states);
			}
			Files.move(SNAPSHOT_TMP, SNAPSHOT, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (Exception e) {
			log.error("snapshot not saved", e);
		}
	}

	@SuppressWarnings("unchecked")
	private void restore() {
		if (!Files.exists(SNAPSHOT)) {
			return;
		}
		try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(SNAPSHOT))) {
			List<EntityState> states = (List<EntityState>) in.readObject();
			for (EntityState state : states) {
				entities.stream().filter(e -> e.getName().equals(state.name())).forEach(e -> e.restore(state));
			}
			log.info("snapshot restored from " + SNAPSHOT.toAbsolutePath() + ", " + states.size() + " entities");
		} catch (Exception e) {
			log.error("snapshot not restored", e);
		}
	}

	private <T extends Entity> T add(T entity) {
		entities.add(entity);
		return entity;
	}

	public void run() {
		Conveyor c1 = add(new Conveyor("C01", 10000, 10));
		Conveyor c2 = add(new Conveyor("C02", 3000, 3));
		Conveyor c3 = add(new Conveyor("C03", 10000, 10));
		Conveyor c4 = add(new Conveyor("C04", 3000, 3));
		Conveyor c5 = add(new Conveyor("C05", 2000, 2));
		Conveyor c6 = add(new Conveyor("C06", 2000, 2));
		Conveyor c7 = add(new Conveyor("C07", 3000, 3));
		Conveyor c8 = add(new Conveyor("C08", 3000, 3));
		Conveyor c9 = add(new Conveyor("C09", 2000, 2));
		Conveyor c10 = add(new Conveyor("C10", 7000, 7));
		Conveyor c11 = add(new Conveyor("C11", 5000, 5));
		Conveyor c12 = add(new Conveyor("C12", 7000, 7));
		Conveyor c13 = add(new Conveyor("C13", 27000, 27));
		Conveyor c14 = add(new Conveyor("C14", 30000, 30));
		Conveyor c15 = add(new Conveyor("C15", 10000, 10));

		Conveyor c16 = add(new Conveyor("C16", 3000, 3));
		Conveyor c17 = add(new Conveyor("C17", 3000, 3));
		Conveyor c18 = add(new Conveyor("C18", 3000, 3));
		Conveyor c19 = add(new Conveyor("C19", 2000, 2));
		Conveyor c20 = add(new Conveyor("C20", 3000, 3));
		Conveyor c21 = add(new Conveyor("C21", 3000, 3));
		Conveyor c22 = add(new Conveyor("C22", 3000, 3));
		Conveyor c23 = add(new Conveyor("C23", 2000, 2));
		Conveyor c24 = add(new Conveyor("C24", 3000, 3));
		Conveyor c25 = add(new Conveyor("C25", 3000, 3));
		Conveyor c26 = add(new Conveyor("C26", 3000, 3));
		Conveyor c27 = add(new Conveyor("C27", 2000, 2));
		Conveyor c28 = add(new Conveyor("C28", 3000, 3));
		Conveyor c29 = add(new Conveyor("C29", 3000, 3));
		Conveyor c30 = add(new Conveyor("C30", 3000, 3));
		Conveyor c31 = add(new Conveyor("C31", 2000, 2));
		Conveyor c32 = add(new Conveyor("C32", 3000, 3));
		Conveyor c33 = add(new Conveyor("C33", 3000, 3));
		Conveyor c34 = add(new Conveyor("C34", 3000, 3));

		InfoPoint ip1 = add(new InfoPoint(Constants.IP1));
		InfoPoint ip2 = add(new InfoPoint(Constants.IP2));
		InfoPoint ip3 = add(new InfoPoint(Constants.IP3));
		InfoPoint e = add(new InfoPoint(Constants.EMPTY));
		InfoPoint ip4 = add(new InfoPoint(Constants.IP4));
		InfoPoint ip5 = add(new InfoPoint(Constants.IP5));
		InfoPoint ip6 = add(new InfoPoint(Constants.IP6));
		InfoPoint ip7 = add(new InfoPoint(Constants.IP7));
		InfoPoint ip8 = add(new InfoPoint(Constants.IP8));

		DecisionPoint dp1 = add(new DecisionPoint(Constants.DP1));
		DecisionPoint dp2 = add(new DecisionPoint(Constants.DP2));
		DecisionPoint dp3 = add(new DecisionPoint(Constants.DP3));
		DecisionPoint dp4 = add(new DecisionPoint(Constants.DP4));
		DecisionPoint dp5 = add(new DecisionPoint(Constants.DP5));
		DecisionPoint dp6 = add(new DecisionPoint(Constants.DP6));
		DecisionPoint dp7 = add(new DecisionPoint(Constants.DP7));
		DecisionPoint dp8 = add(new DecisionPoint(Constants.DP8));

		SourceTransportPoint tps1 = add(new SourceTransportPoint(Constants.TPS1, MfsMessages.REQUEST_QUEUE));
		SourceTransportPoint tps2 = add(new SourceTransportPoint(Constants.TPS2, MfsMessages.DECANTING_REQUEST_QUEUE));

		TargetTransportPoint tpt1 = add(new TargetTransportPoint(Constants.TPT1));

		StationPoint ptts1 = add(new StationPoint(Constants.PTTS1));
		StationPoint pttt1 = add(new StationPoint(Constants.PTTT1));
		StationPoint ptts2 = add(new StationPoint(Constants.PTTS2));
		StationPoint pttt2 = add(new StationPoint(Constants.PTTT2));
		StationPoint ptts3 = add(new StationPoint(Constants.PTTS3));
		StationPoint pttt3 = add(new StationPoint(Constants.PTTT3));
		StationPoint out = add(new StationPoint(Constants.OUT));

		c6.getSource().add(tps1);
		ip1.getSource().add(c6);
		ip1.getSource().add(c4);

		c1.getSource().add(ip1);
		dp3.getSource().add(c1);
		dp1.getSource().add(c10);
		c10.getSource().add(dp3);

		// DP3: 0 = C10 spre DP1 (default), 1 = C11 spre EMPTY
		dp3.getTarget().add(c10);
		dp3.getTarget().add(c11);

		// DP1: 0 = C7 spre PTTS1 (default), 1 = C2 spre IP2 (bucla)
		dp1.getTarget().add(c7);
		dp1.getTarget().add(c2);

		c2.getSource().add(dp1);
		c7.getSource().add(dp1);

		ip2.getSource().add(c2);
		ip2.getSource().add(c8);

		c19.getSource().add(ip2);
		dp5.getSource().add(c19);
		zone(dp5, c20, ptts2, c21, c22, ip5);
		c23.getSource().add(ip5);
		dp7.getSource().add(c23);
		zone(dp7, c24, ptts3, c25, c26, ip7);

		c3.getSource().add(ip7);
		// IP3 pe C03: primeste de pe C03 (bucla) si de pe C14 (decanting)
		ip3.getSource().add(c3);
		ip3.getSource().add(c14);
		c14.getSource().add(tps2);
		c15.getSource().add(ip3);
		dp2.getSource().add(c15);

		// DP2: 0 = C5 spre TPT1 (default), 1 = C4 spre IP1 (bucla)
		dp2.getTarget().add(c5);
		dp2.getTarget().add(c4);

		c4.getSource().add(dp2);
		c5.getSource().add(dp2);

		tpt1.getSource().add(c5);

		out.getSource().add(c9);
		c13.getSource().add(out);

		c11.getSource().add(dp3);
		e.getSource().add(c11);
		e.getSource().add(c13);
		c12.getSource().add(e);

		ptts1.getSource().add(c7);
		c8.getSource().add(ptts1);

		dp4.getSource().add(c12);
		zone(dp4, c16, pttt1, c17, c18, ip4);
		c27.getSource().add(ip4);
		dp6.getSource().add(c27);
		zone(dp6, c28, pttt2, c29, c30, ip6);
		c31.getSource().add(ip6);
		dp8.getSource().add(c31);
		zone(dp8, c32, pttt3, c33, c34, ip8);
		c9.getSource().add(ip8);

		restore();
		entities.forEach(Entity::start);
	}

	private void zone(DecisionPoint dp, Conveyor in, StationPoint station, Conveyor out, Conveyor bypass,
			InfoPoint ip) {
		dp.getTarget().add(in);
		dp.getTarget().add(bypass);
		in.getSource().add(dp);
		bypass.getSource().add(dp);
		station.getSource().add(in);
		out.getSource().add(station);
		ip.getSource().add(out);
		ip.getSource().add(bypass);
	}

	public String getJSON() {
		List<String> items = new ArrayList<>();
		for (int i = 0; i < entities.size(); i++) {
			items.add(entities.get(i).toJSON(i));
		}
		return "[\n" + String.join(",\n", items) + "\n]";
	}

	public List<Entity> getEntities() {
		return entities;
	}
}