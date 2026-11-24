package ro.demo.usersbackend.server.mfs;

import ro.demo.usersbackend.common.Constants;

public class DP4Handler extends DecisionPointHandler {
	public DP4Handler() {
		super(Constants.DP4);
	}

	@Override
	protected int decide(String loadUnit, String target) {
		return decideForZone(loadUnit, target, Constants.PTTT1);
	}
}