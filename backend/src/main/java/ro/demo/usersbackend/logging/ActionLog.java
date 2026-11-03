package ro.demo.usersbackend.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ActionLog {
	public static final Logger LOG = LogManager.getLogger("application");

	private ActionLog() {
	}
}
