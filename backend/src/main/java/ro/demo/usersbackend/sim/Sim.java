package ro.demo.usersbackend.sim;

public class Sim {
	public static void main(String[] args) throws InterruptedException {
		Conveyor c1 = new Conveyor("C1", 30000, 30);
		Conveyor c2 = new Conveyor("C2", 2000, 2);
		Conveyor c3 = new Conveyor("C3", 30000, 30);
		Conveyor c4 = new Conveyor("C4", 2000, 2);
		Conveyor c5 = new Conveyor("C5", 2000, 2);
		Conveyor c6 = new Conveyor("C6", 2000, 2);
		Conveyor c7 = new Conveyor("C7", 2000, 2);
		Conveyor c8 = new Conveyor("C8", 2000, 2);
		Conveyor c9 = new Conveyor("C9", 10000, 10);

		InfoPoint ip1 = new InfoPoint("IP1");
		InfoPoint ip2 = new InfoPoint("IP2");
		InfoPoint ip3 = new InfoPoint("IP3");

		DecisionPoint dp1 = new DecisionPoint("DP1");
		DecisionPoint dp2 = new DecisionPoint("DP2");

		SourceTransportPoint tps1 = new SourceTransportPoint("TPS1");

		TargetTransportPoint tpt1 = new TargetTransportPoint("TPT1");

		c6.getSource().add(tps1);
		ip1.getSource().add(c6);
		ip1.getSource().add(c4);

		c1.getSource().add(ip1);
		dp1.getSource().add(c1);

		dp1.setYesTarget(c7);
		dp1.setNoTarget(c2);
		c2.getSource().add(dp1);
		c7.getSource().add(dp1);

		ip2.getSource().add(c2);
		ip2.getSource().add(c8);

		c3.getSource().add(ip2);
		dp2.getSource().add(c3);

		dp2.setYesTarget(c5);
		dp2.setNoTarget(c4);
		c4.getSource().add(dp2);
		c5.getSource().add(dp2);

		tpt1.getSource().add(c5);

		ip3.getSource().add(c9);

		// tps1.get("123");
	}
}
