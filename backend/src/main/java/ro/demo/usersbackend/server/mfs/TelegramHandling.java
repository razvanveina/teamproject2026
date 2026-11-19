package ro.demo.usersbackend.server.mfs;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ro.demo.usersbackend.common.Constants;

public class TelegramHandling {
	public static final Logger log = LogManager.getLogger("application");

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
			Map.entry(Constants.TPS2, new TPS2Handler()));

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