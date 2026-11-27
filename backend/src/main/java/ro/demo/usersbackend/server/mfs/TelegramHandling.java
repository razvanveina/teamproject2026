package ro.demo.usersbackend.server.mfs;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ro.demo.usersbackend.common.Constants;

public class TelegramHandling {
	private static final Logger log = LogManager.getLogger(TelegramHandling.class);

	private static TelegramHandling instance;

	private TelegramHandling() {
	}

	public static TelegramHandling getInstance() {
		if (instance == null) {
			instance = new TelegramHandling();
		}
		return instance;
	}

	// punctul de pe banda -> handlerul care ii proceseaza telegramele
	private static final Map<String, TelegramHandler> HANDLERS = Map.ofEntries(
			Map.entry(Constants.TPS1, new TPS1Handler()), Map.entry(Constants.TPT1, new TPT1Handler()),
			Map.entry(Constants.IP1, new IP1Handler()), Map.entry(Constants.IP2, new IP2Handler()),
			Map.entry(Constants.DP1, new DP1Handler()), Map.entry(Constants.DP2, new DP2Handler()),
			Map.entry(Constants.DP3, new DP3Handler()), Map.entry(Constants.PTTS1, new PTTS1Handler()),
			Map.entry(Constants.PTTT1, new PTTT1Handler()), Map.entry(Constants.OUT, new OUTHandler()),
			Map.entry(Constants.EMPTY, new EMPTYHandler()), Map.entry(Constants.IP3, new IP3Handler()),
			Map.entry(Constants.TPS2, new TPS2Handler()), Map.entry(Constants.IP4, new IP4Handler()),
			Map.entry(Constants.IP5, new IP5Handler()), Map.entry(Constants.IP6, new IP6Handler()),
			Map.entry(Constants.IP7, new IP7Handler()), Map.entry(Constants.IP8, new IP8Handler()),
			Map.entry(Constants.DP4, new DP4Handler()), Map.entry(Constants.DP5, new DP5Handler()),
			Map.entry(Constants.DP6, new DP6Handler()), Map.entry(Constants.DP7, new DP7Handler()),
			Map.entry(Constants.DP8, new DP8Handler()), Map.entry(Constants.PTTS2, new PTTS2Handler()),
			Map.entry(Constants.PTTS3, new PTTS3Handler()), Map.entry(Constants.PTTT2, new PTTT2Handler()),
			Map.entry(Constants.PTTT3, new PTTT3Handler()),
			Map.entry(Constants.GP1, new GatePointHandler(Constants.GP1, Constants.CONVEYOR_GATES.get(0))),
			Map.entry(Constants.GP2, new GatePointHandler(Constants.GP2, Constants.CONVEYOR_GATES.get(1))),
			Map.entry(Constants.GP3, new GatePointHandler(Constants.GP3, Constants.CONVEYOR_GATES.get(2))),
			Map.entry(Constants.GP4, new GatePointHandler(Constants.GP4, null)),
			Map.entry(Constants.CONVEYOR_GATES.get(0), new GateHandler(Constants.CONVEYOR_GATES.get(0))),
			Map.entry(Constants.CONVEYOR_GATES.get(1), new GateHandler(Constants.CONVEYOR_GATES.get(1))),
			Map.entry(Constants.CONVEYOR_GATES.get(2), new GateHandler(Constants.CONVEYOR_GATES.get(2))),
			Map.entry(Constants.CONVEYOR_GATES.get(3), new GateHandler(Constants.CONVEYOR_GATES.get(3))));

	// ia ca parametrii punctul si telegrama lui
	// gaseste handleru punctului si ii paseaza telegrama
	public void route(String point, String telegram) throws Exception {
		TelegramHandler handler = HANDLERS.get(point);
		if (handler == null) {
			log.warn("TelegramHandling: no telegram for point {}", point);
			return;
		}
		handler.handle(telegram);
	}
}