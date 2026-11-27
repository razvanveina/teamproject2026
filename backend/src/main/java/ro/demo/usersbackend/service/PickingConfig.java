package ro.demo.usersbackend.service;

import ro.demo.usersbackend.common.Constants;

public enum PickingConfig {
	PICKING_1(Constants.PTTS1, Constants.DP5, Constants.DP1), PICKING_2(Constants.PTTS2, Constants.DP7, Constants.DP5),
	PICKING_3(Constants.PTTS3, Constants.DP2, Constants.DP7);

	private String pickingTargetLocation;
	private String targetLocationAfterConfirm;
	private String sourceLocation;

	PickingConfig(String pickingTargetLocation, String targetLocationAfterConfirm, String sourceLocation) {
		this.pickingTargetLocation = pickingTargetLocation;
		this.targetLocationAfterConfirm = targetLocationAfterConfirm;
		this.sourceLocation = sourceLocation;
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

	public static PickingConfig getBySourceLocation(String sourceLocation) {
		for (PickingConfig config : PickingConfig.values()) {
			if (config.getSourceLocation().equals(sourceLocation)) {
				return config;
			}
		}
		return null;
	}

	public String getSourceLocation() {
		return sourceLocation;
	}

}
