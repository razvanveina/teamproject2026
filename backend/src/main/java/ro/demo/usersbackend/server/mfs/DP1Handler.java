package ro.demo.usersbackend.server.mfs;

public class DP1Handler extends DecisionPointHandler{
	public DP1Handler() {
		super("DP1");
	}
	
	// 0 = DP1 -> PTTS1 (C7) ; 1 = DP1 -> IP2 (C2)
	@Override
	protected int decide(String loadUnit) {
		return 0;
	}
}