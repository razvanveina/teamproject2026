package ro.demo.usersbackend.server.mfs;

import ro.demo.usersbackend.common.Constants;

public class IP8Handler extends DecisionPointHandler {
	public IP8Handler() {
		super(Constants.IP8);
	}

	// 0 = IP8 -> OUT (C09) ; 1 = IP8 -> EMPTY (C13)
	// la OUT merg doar tote-urile cu TO spre OUT (comenzi complete), restul (goale)
	// se intorc direct la EMPTY
	@Override
	protected int decide(String loadUnit, String target) {
		return Constants.OUT.equals(target) ? 0 : 1;
	}
}