package ro.demo.usersbackend.server.mfs;

import ro.demo.usersbackend.common.Constants;

public class DP1Handler extends DecisionPointHandler {
	public DP1Handler() {
		super(Constants.DP1);
	}

	// 0 = DP1 -> PTTS1 (C7) ; 1 = DP1 -> IP2 (C2)
	// in statie intra doar tote-ul care are TO spre PTTS1
	@Override
	protected int decide(String loadUnit, String target) {
		return decideForStation(loadUnit, target, Constants.PTTS1);
	}
}