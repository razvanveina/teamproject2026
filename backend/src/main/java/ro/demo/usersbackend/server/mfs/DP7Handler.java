package ro.demo.usersbackend.server.mfs;

import ro.demo.usersbackend.common.Constants;

public class DP7Handler extends DecisionPointHandler {
	public DP7Handler() {
		super(Constants.DP7);
	}

	@Override
	protected int decide(String loadUnit, String target) {
		return Constants.PTTS3.equals(target) ? 0 : 1;
	}
}