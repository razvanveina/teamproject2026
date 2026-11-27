package ro.demo.usersbackend.service;

import ro.demo.usersbackend.common.Constants;

public enum PickingConfig {
	PICKING_1(Constants.PTTS1, Constants.DP5), PICKING_2(Constants.PTTS2, Constants.DP7),
	PICKING_3(Constants.PTTS3, Constants.DP1);

	private String pickingTargetLocation;
	private String targetLocationAfterConfirm;

	PickingConfig(String pickingTargetLocation, String targetLocationAfterConfirm) {
		this.pickingTargetLocation = pickingTargetLocation;
		this.targetLocationAfterConfirm = targetLocationAfterConfirm;
	}

	public String getPickingTargetLocation() {
		return pickingTargetLocation;
	}

	public String getTargetLocationAfterConfirm() {
		return targetLocationAfterConfirm;
	}

	public static PickingConfig getByPickingTargetLocation(String pickingTargetLocation) {
		for (PickingConfig config : PickingConfig.values()) {
			if (config.getPickingTargetLocation().equals(pickingTargetLocation)) {
				return config;
			}
		}
		return null;
	}

}
