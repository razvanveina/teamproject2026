package ro.demo.usersbackend.sim;

public class Sim {
	public static void main(String[] args) throws InterruptedException {
		Conveyor c1 = new Conveyor("C01");
		Conveyor c2 = new Conveyor("C02");

		InfoPoint p1 = new InfoPoint("PICKING_BUFFER");
		InfoPoint p2 = new InfoPoint("PICKING");

		TransportPoint tps1 = new TransportPoint("TPS1");

	}
}
