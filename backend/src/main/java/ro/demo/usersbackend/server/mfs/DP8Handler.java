package ro.demo.usersbackend.server.mfs;

import ro.demo.usersbackend.common.Constants;

public class DP8Handler extends DecisionPointHandler {
	public DP8Handler() {
		super(Constants.DP8);
	}

	@Override
	protected int decide(String loadUnit, String target) {
		return decideForZone(loadUnit, target, Constants.PTTT3);
	}
}