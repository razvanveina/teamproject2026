package ro.demo.usersbackend.sim;

public class Sim {
	public static void main(String[] args) throws InterruptedException {
		Conveyor c1 = new Conveyor("C01");
		Conveyor c2 = new Conveyor("C02");

		InfoPoint p1 = new InfoPoint("PICKING_BUFFER");
		InfoPoint p2 = new InfoPoint("PICKING");

		p1.getSource().add(c1);
		p2.getSource().add(c2);
		c2.getSource().add(p1);

		p1.getTarget().add(c2);
		c1.getTarget().add(p2);

		c1.createLU("123");
		Thread.sleep(1000);
		c1.createLU("124");
	}
}
