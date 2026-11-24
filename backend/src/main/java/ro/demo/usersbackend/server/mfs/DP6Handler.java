package ro.demo.usersbackend.server.mfs;

import ro.demo.usersbackend.common.Constants;

public class DP6Handler extends DecisionPointHandler {
	public DP6Handler() {
		super(Constants.DP6);
	}

	@Override
	protected int decide(String loadUnit, String target) {
		return decideForZone(loadUnit, target, Constants.PTTT2);
	}
}