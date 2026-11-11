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

public class Sim {

	private static final Path SNAPSHOT = Path.of("sim-snapshot.bin");
	private static final Path SNAPSHOT_TMP = Path.of("sim-snapshot.bin.tmp");

	private final List<Entity> entities = new ArrayList<>();

	public static void main(String[] args) throws IOException {
		Sim sim = new Sim();
		sim.run();
		Runtime.getRuntime().addShutdownHook(new Thread(sim::save));
		Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(sim::save, 5, 5, TimeUnit.SECONDS);
		new Thread(() -> {
			Scanner in = new Scanner(System.in);
			while (in.hasNextLine()) {
				if (in.nextLine().trim().equalsIgnoreCase("stop")) {
					System.exit(0);
				}
			}
		}).start();
		new CustomHttpServer(sim);
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
			System.out.println("snapshot not saved " + e);
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
			System.out.println(
					"snapshot restored from " + SNAPSHOT.toAbsolutePath() + ", " + states.size() + " entities");
		} catch (Exception e) {
			System.out.println("snapshot not restored " + e);
		}
	}

	private <T extends Entity> T add(T entity) {
		entities.add(entity);
		return entity;
	}

	public void run() {
		Conveyor c1 = add(new Conveyor("C01", 30000, 30));
		Conveyor c2 = add(new Conveyor("C02", 2000, 2));
		Conveyor c3 = add(new Conveyor("C03", 30000, 30));
		Conveyor c4 = add(new Conveyor("C04", 2000, 2));
		Conveyor c5 = add(new Conveyor("C05", 2000, 2));
		Conveyor c6 = add(new Conveyor("C06", 2000, 2));
		Conveyor c7 = add(new Conveyor("C07", 2000, 2));
		Conveyor c8 = add(new Conveyor("C08", 2000, 2));
		Conveyor c9 = add(new Conveyor("C09", 10000, 10));
		Conveyor c10 = add(new Conveyor("C10", 10000, 10));
		Conveyor c11 = add(new Conveyor("C11", 10000, 10));
		Conveyor c12 = add(new Conveyor("C12", 10000, 10));
		Conveyor c13 = add(new Conveyor("C13", 10000, 10));

		InfoPoint ip1 = add(new InfoPoint("IP1"));
		InfoPoint ip2 = add(new InfoPoint("IP2"));
		InfoPoint ip3 = add(new InfoPoint("IP3"));
		InfoPoint e = add(new InfoPoint("EMPTY"));

		DecisionPoint dp1 = add(new DecisionPoint("DP1"));
		DecisionPoint dp2 = add(new DecisionPoint("DP2"));
		DecisionPoint dp3 = add(new DecisionPoint("DP3"));

		SourceTransportPoint tps1 = add(new SourceTransportPoint("TPS1"));

		TargetTransportPoint tpt1 = add(new TargetTransportPoint("TPT1"));

		StationPoint ptts1 = add(new StationPoint("PTTS1"));
		StationPoint pttt1 = add(new StationPoint("PTTT1"));

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

		c3.getSource().add(ip2);
		dp2.getSource().add(c3);

		// DP2: 0 = C5 spre TPT1 (default), 1 = C4 spre IP1 (bucla)
		dp2.getTarget().add(c5);
		dp2.getTarget().add(c4);

		c4.getSource().add(dp2);
		c5.getSource().add(dp2);

		tpt1.getSource().add(c5);

		ip3.getSource().add(c9);
		c13.getSource().add(ip3);

		c11.getSource().add(dp3);
		e.getSource().add(c11);
		e.getSource().add(c13);
		c12.getSource().add(e);

		ptts1.getSource().add(c7);
		c8.getSource().add(ptts1);

		pttt1.getSource().add(c12);
		c9.getSource().add(pttt1);

		restore();
		entities.forEach(Entity::start);
	}

	public String getJSON() {
		List<String> items = new ArrayList<>();
		for (int i = 0; i < entities.size(); i++) {
			items.add(entities.get(i).toJSON(i));
		}
		return "[\n" + String.join(",\n", items) + "\n]";
	}
}