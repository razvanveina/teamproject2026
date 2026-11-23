package ro.demo.usersbackend.server.mfs;

// GP1-GP4 de pe linia de gate-uri: cartonul coboara la gate-ul de sub punct
// doar daca TO-ul lui activ are ca target acel gate
// 0 = continua pe linie ; 1 = coboara la gate
public class GatePointHandler extends DecisionPointHandler {
	private final String gate;

	// gate = gate-ul de sub punct; null pentru GP4, capat de linie cu o singura
	// iesire
	public GatePointHandler(String point, String gate) {
		super(point);
		this.gate = gate;
	}

	@Override
	protected int decide(String loadUnit, String target) {
		return gate != null && gate.equals(target) ? 1 : 0;
	}
}