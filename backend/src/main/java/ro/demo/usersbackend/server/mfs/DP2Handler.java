package ro.demo.usersbackend.server.mfs;

public class DP2Handler extends DecisionPointHandler{
	public DP2Handler() {
		super("DP2");
	}
	
	// 0 = DP2 -> TPT1 (C5) ; 1 = DP2 -> IP1 (C4)
	@Override
	protected int decide(String loadUnit) {
		return 0;
	}
}