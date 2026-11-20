package ro.demo.usersbackend.server.mfs;

import ro.demo.usersbackend.common.Constants;

public class DP5Handler extends DecisionPointHandler {
	public DP5Handler() {
		super(Constants.DP5);
	}

	@Override
	protected int decide(String loadUnit, String target) {
		return Constants.PTTS2.equals(target) ? 0 : 1;
	}
}