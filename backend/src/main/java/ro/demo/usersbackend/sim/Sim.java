package ro.demo.usersbackend.sim;

import java.awt.Point;
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
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import jakarta.jms.TemporaryQueue;
import jakarta.jms.TextMessage;
import ro.demo.usersbackend.common.Constants;
import ro.demo.usersbackend.messaging.Broker;
import ro.demo.usersbackend.messaging.MfsMessages;
import ro.demo.usersbackend.messaging.MfsMessages.SetupLoadUnit;

public class Sim {

	private static final Path SNAPSHOT = Path.of("sim-snapshot.bin");
	private static final Path SNAPSHOT_TMP = Path.of("sim-snapshot.bin.tmp");

	private final Logger log = LogManager.getLogger("application");
	private final List<Entity> entities = new ArrayList<>();

	private Queue outQueue;
	private JMSConsumer consumer;
	private JMSContext context;

	private boolean skipSave = false;

	public static void main(String[] args) throws IOException {
		System.setProperty(Constants.LOG_CONFIGURATION_PROPERTY, Constants.LOG_CONFIGURATION_FILE);
		Sim sim = new Sim();
		sim.run(args);
		SimWindow window = new SimWindow(sim);
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
			context = new ActiveMQConnectionFactory(Broker.CLIENT_URL).createContext();
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
							if (e.getName().equals(s[2])) {
								e.addLoadUnit(new SimLoadUnit(s[1], s.length > 3 ? s[3] : Constants.TOTE_TYPE));
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
		if (skipSave) {
			return;
		}

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

	void clearLoadUnits() {
		Lock lock = Entity.LOCK.writeLock();
		try {
			if (!lock.tryLock(2, TimeUnit.SECONDS)) {
				log.warn("simulator reset skipped, entities busy");
				return;
			}
			try {
				entities.forEach(Entity::clear);
			} finally {
				lock.unlock();
			}
			log.info("simulator reset: all load units removed");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
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

	private Entity entityNamed(String name) {
		for (Entity entity : entities) {
			if (entity.getName().equals(name)) {
				return entity;
			}
		}
		return null;
	}

	// punctul e ocupat: load unit-ul asteapta pe o banda care intra in punct
	// (IP-urile au doua)
	private boolean queueBeforePoint(Entity point, SimLoadUnit loadUnit) {
		for (Entity source : point.getSource()) {
			if (source instanceof Conveyor conveyor && conveyor.queueAtEnd(loadUnit)) {
				return true;
			}
		}
		return false;
	}

	// butonul Setup: golim simulatorul si punem pe puncte load unit-urile pe care
	// DB-ul le are acolo
	void setupFromServer() {
		List<SetupLoadUnit> loadUnits = requestSetup();
		if (loadUnits == null) {
			return;
		}
		clearLoadUnits();

		int placed = 0;
		int queued = 0;
		// sub write lock, ca nimic sa nu se miste pe benzi cat le umplem
		Lock lock = Entity.LOCK.writeLock();
		lock.lock();
		try {
			for (SetupLoadUnit setupLoadUnit : loadUnits) {
				Entity point = entityNamed(setupLoadUnit.point());
				if (point == null) {
					log.warn("setup: point {} is not in the simulator, {} skipped", setupLoadUnit.point(),
							setupLoadUnit.loadUnit());
					continue;
				}
				SimLoadUnit simLoadUnit = new SimLoadUnit(setupLoadUnit.loadUnit(), setupLoadUnit.loadUnitType());
				if (point.addLoadUnit(simLoadUnit)) {
					placed++;
				} else if (queueBeforePoint(point, simLoadUnit)) {
					queued++;
					log.info("setup: {} is busy, {} waits on the belt before it", setupLoadUnit.point(),
							setupLoadUnit.loadUnit());
				} else {
					log.warn("setup: {} and the belts before it are full, {} skipped", setupLoadUnit.point(),
							setupLoadUnit.loadUnit());
				}
			}
		} finally {
			lock.unlock();
		}
		log.info("setup: {} placed on points, {} waiting on belts, {} load units from the server", placed, queued,
				loadUnits.size());
	}

	private List<SetupLoadUnit> requestSetup() {
		try (JMSContext setupContext = new ActiveMQConnectionFactory(Broker.URL).createContext()) {
			TemporaryQueue replyQueue = setupContext.createTemporaryQueue();
			TextMessage request = setupContext.createTextMessage(Constants.SETUP);
			request.setJMSReplyTo(replyQueue);

			try (JMSConsumer replies = setupContext.createConsumer(replyQueue)) {
				setupContext.createProducer().send(setupContext.createQueue(MfsMessages.SETUP_REQUEST_QUEUE), request);
				// ActiveMQ Classic nu suporta receiveBody, luam mesajul si citim textul
				Message reply = replies.receive(Constants.SETUP_TIMEOUT);
				if (!(reply instanceof TextMessage text)) {
					log.warn("setup: no reply from the server in {} ms", Constants.SETUP_TIMEOUT);
					return null;
				}
				return List.of(MfsMessages.fromJson(text.getText(), SetupLoadUnit[].class));
			}
		} catch (JMSException e) {
			log.error("setup: request failed", e);
			return null;
		}
	}

	public void run(String[] args) {
		Conveyor c1 = add(new Conveyor("C01", 10, new Point(58, 440), 560, 300, 120, 300, 120, 560));
		Conveyor c2 = add(new Conveyor("C02", 3, new Point(654, 524), 560, 560, 768, 560));
		Conveyor c3 = add(new Conveyor("C03", 10, new Point(1536, 505), 1408, 560, 1500, 560, 1500, 430));
		Conveyor c4 = add(new Conveyor("C04", 3, new Point(654, 345), 768, 300, 560, 300));
		Conveyor c5 = add(new Conveyor("C05", 2, new Point(804, 220), 768, 300, 768, 130));
		Conveyor c6 = add(new Conveyor("C06", 2, new Point(498, 220), 560, 130, 560, 300));
		Conveyor c7 = add(new Conveyor("C07", 3, new Point(498, 630), 560, 560, 560, 690, 664, 690));
		Conveyor c8 = add(new Conveyor("C08", 3, new Point(804, 630), 664, 690, 768, 690, 768, 560));
		Conveyor c9 = add(new Conveyor("C09", 2, new Point(1450, 945), 1408, 900, 1540, 900));
		Conveyor c10 = add(new Conveyor("C10", 7, new Point(325, 605), 120, 560, 560, 560));
		Conveyor c11 = add(new Conveyor("C11", 5, new Point(58, 730), 120, 560, 120, 900));
		Conveyor c12 = add(new Conveyor("C12", 7, new Point(325, 945), 120, 900, 560, 900));
		Conveyor c13 = add(new Conveyor("C13", 27, new Point(790, 1045), 1540, 900, 1540, 1000, 120, 1000, 120, 900));
		Conveyor c14 = add(new Conveyor("C14", 2, new Point(1580, 392), 1660, 430, 1500, 430));
		Conveyor c15 = add(new Conveyor("C15", 10, new Point(1130, 345), 1500, 430, 1500, 300, 768, 300));

		Conveyor c16 = add(
				new Conveyor("C16", Constants.PTTT_LANE_SLOTS, new Point(498, 840), 560, 900, 560, 770, 664, 770));
		Conveyor c17 = add(new Conveyor("C17", 3, new Point(804, 805), 664, 770, 768, 770, 768, 900));
		Conveyor c18 = add(new Conveyor("C18", 3, new Point(650, 945), 560, 900, 768, 900));
		Conveyor c19 = add(new Conveyor("C19", 2, new Point(808, 524), 768, 560, 880, 560));
		Conveyor c20 = add(new Conveyor("C20", 3, new Point(828, 705), 880, 560, 880, 690, 984, 690));
		Conveyor c21 = add(new Conveyor("C21", 3, new Point(1124, 630), 984, 690, 1088, 690, 1088, 560));
		Conveyor c22 = add(new Conveyor("C22", 3, new Point(970, 524), 880, 560, 1088, 560));
		Conveyor c23 = add(new Conveyor("C23", 2, new Point(1128, 524), 1088, 560, 1200, 560));
		Conveyor c24 = add(new Conveyor("C24", 3, new Point(1148, 705), 1200, 560, 1200, 690, 1304, 690));
		Conveyor c25 = add(new Conveyor("C25", 3, new Point(1444, 630), 1304, 690, 1408, 690, 1408, 560));
		Conveyor c26 = add(new Conveyor("C26", 3, new Point(1290, 524), 1200, 560, 1408, 560));
		Conveyor c27 = add(new Conveyor("C27", 2, new Point(808, 945), 768, 900, 880, 900));
		Conveyor c28 = add(
				new Conveyor("C28", Constants.PTTT_LANE_SLOTS, new Point(828, 865), 880, 900, 880, 770, 984, 770));
		Conveyor c29 = add(new Conveyor("C29", 3, new Point(1124, 805), 984, 770, 1088, 770, 1088, 900));
		Conveyor c30 = add(new Conveyor("C30", 3, new Point(970, 945), 880, 900, 1088, 900));
		Conveyor c31 = add(new Conveyor("C31", 2, new Point(1128, 945), 1088, 900, 1200, 900));
		Conveyor c32 = add(
				new Conveyor("C32", Constants.PTTT_LANE_SLOTS, new Point(1148, 865), 1200, 900, 1200, 770, 1304, 770));
		Conveyor c33 = add(new Conveyor("C33", 3, new Point(1444, 805), 1304, 770, 1408, 770, 1408, 900));
		Conveyor c34 = add(new Conveyor("C34", 3, new Point(1290, 945), 1200, 900, 1408, 900));
		Conveyor c35 = add(new Conveyor("C35", 5, new Point(1640, 1010), 1540, 900, 1700, 900, 1700, 1120, 1600, 1120));
		Conveyor c36 = add(new Conveyor("C36", 3, new Point(1490, 1165), 1600, 1120, 1380, 1120));
		Conveyor c37 = add(new Conveyor("C37", 3, new Point(1270, 1165), 1380, 1120, 1160, 1120));
		Conveyor c38 = add(new Conveyor("C38", 3, new Point(1050, 1165), 1160, 1120, 940, 1120));
		Conveyor c39 = add(new Conveyor("C39", 5, new Point(1542, 1280), 1600, 1120, 1600, 1440));
		Conveyor c40 = add(new Conveyor("C40", 5, new Point(1322, 1280), 1380, 1120, 1380, 1440));
		Conveyor c41 = add(new Conveyor("C41", 5, new Point(1102, 1280), 1160, 1120, 1160, 1440));
		Conveyor c42 = add(new Conveyor("C42", 5, new Point(882, 1280), 940, 1120, 940, 1440));

		InfoPoint ip1 = add(new InfoPoint(Constants.IP1, 560, 300, true));
		InfoPoint ip2 = add(new InfoPoint(Constants.IP2, 768, 560));
		InfoPoint ip3 = add(new InfoPoint(Constants.IP3, 1500, 430));
		InfoPoint ip4 = add(new InfoPoint(Constants.IP4, 768, 900, true));
		InfoPoint ip5 = add(new InfoPoint(Constants.IP5, 1088, 560));
		InfoPoint ip6 = add(new InfoPoint(Constants.IP6, 1088, 900, true));
		InfoPoint ip7 = add(new InfoPoint(Constants.IP7, 1408, 560));
		InfoPoint ip8 = add(new InfoPoint(Constants.IP8, 1408, 900, true));
		InfoPoint e = add(new InfoPoint(Constants.EMPTY, 120, 900));

		DecisionPoint dp1 = add(new DecisionPoint(Constants.DP1, 560, 560));
		DecisionPoint dp2 = add(new DecisionPoint(Constants.DP2, 768, 300, true));
		DecisionPoint dp3 = add(new DecisionPoint(Constants.DP3, 120, 560));
		DecisionPoint dp4 = add(new DecisionPoint(Constants.DP4, 560, 900, true));
		DecisionPoint dp5 = add(new DecisionPoint(Constants.DP5, 880, 560));
		DecisionPoint dp6 = add(new DecisionPoint(Constants.DP6, 880, 900, true));
		DecisionPoint dp7 = add(new DecisionPoint(Constants.DP7, 1200, 560));
		DecisionPoint dp8 = add(new DecisionPoint(Constants.DP8, 1200, 900, true));
		DecisionPoint gp1 = add(new DecisionPoint(Constants.GP1, 1600, 1120));
		DecisionPoint gp2 = add(new DecisionPoint(Constants.GP2, 1380, 1120));
		DecisionPoint gp3 = add(new DecisionPoint(Constants.GP3, 1160, 1120));
		DecisionPoint gp4 = add(new DecisionPoint(Constants.GP4, 940, 1120));

		SourceTransportPoint tps1 = add(new SourceTransportPoint(Constants.TPS1, MfsMessages.REQUEST_QUEUE, 560, 130));
		SourceTransportPoint tps2 = add(
				new SourceTransportPoint(Constants.TPS2, MfsMessages.DECANTING_REQUEST_QUEUE, 1660, 430));

		TargetTransportPoint tpt1 = add(new TargetTransportPoint(Constants.TPT1, 768, 130));

		StationPoint ptts1 = add(new StationPoint(Constants.PTTS1, 664, 690));
		StationPoint pttt1 = add(new StationPoint(Constants.PTTT1, 664, 770));
		StationPoint ptts2 = add(new StationPoint(Constants.PTTS2, 984, 690));
		StationPoint pttt2 = add(new StationPoint(Constants.PTTT2, 984, 770));
		StationPoint ptts3 = add(new StationPoint(Constants.PTTS3, 1304, 690));
		StationPoint pttt3 = add(new StationPoint(Constants.PTTT3, 1304, 770));
		StationPoint out = add(new StationPoint(Constants.OUT, 1540, 900));
		GatePoint gate1 = add(new GatePoint(Constants.CONVEYOR_GATES.get(0), 1600, 1440, true));
		GatePoint gate2 = add(new GatePoint(Constants.CONVEYOR_GATES.get(1), 1380, 1440, true));
		GatePoint gate3 = add(new GatePoint(Constants.CONVEYOR_GATES.get(2), 1160, 1440, true));
		GatePoint gate4 = add(new GatePoint(Constants.CONVEYOR_GATES.get(3), 940, 1440, true));

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
		zone(dp1, c7, ptts1, c8, c2, ip2);

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

		dp4.getSource().add(c12);
		zone(dp4, c16, pttt1, c17, c18, ip4);
		c27.getSource().add(ip4);
		dp6.getSource().add(c27);
		zone(dp6, c28, pttt2, c29, c30, ip6);
		c31.getSource().add(ip6);
		dp8.getSource().add(c31);
		zone(dp8, c32, pttt3, c33, c34, ip8);
		c9.getSource().add(ip8);

		gp1.getSource().add(c35);
		c36.getSource().add(gp1);
		gp2.getSource().add(c36);
		c37.getSource().add(gp2);
		gp3.getSource().add(c37);
		c38.getSource().add(gp3);
		gp4.getSource().add(c38);

		gp1.getTarget().add(c36);
		gp1.getTarget().add(c39);
		gp2.getTarget().add(c37);
		gp2.getTarget().add(c40);
		gp3.getTarget().add(c38);
		gp3.getTarget().add(c41);
		gp4.getTarget().add(c42);

		c39.getSource().add(gp1);
		c40.getSource().add(gp2);
		c41.getSource().add(gp3);
		c42.getSource().add(gp4);

		gate1.getSource().add(c39);
		gate2.getSource().add(c40);
		gate3.getSource().add(c41);
		gate4.getSource().add(c42);

		if (args.length > 0) {
			if ("--new".equals(args[0])) {
				try {
					Files.delete(SNAPSHOT);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			} else if ("--skipsave".equals(args[0])) {
				skipSave = true;
			}
		} else {
			restore();
		}
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