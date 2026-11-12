package ro.demo.usersbackend.server.mfs;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TelegramHandling {
	public static final Logger log = LogManager.getLogger("application");

	private static final Map<String, TelegramHandler> HANDLERS = Map.ofEntries(
			Map.entry("TPS1", new TPS1Handler()),
			Map.entry("TPT1", new TPT1Handler()), 
			Map.entry("IP1", new IP1Handler()),
			Map.entry("DP1", new DP1Handler()), 
			Map.entry("PTTS1", new PTTS1Handler()),
			Map.entry("PTTT1", new PTTT1Handler()), 
			Map.entry("DP2", new DP2Handler()),
			Map.entry("IP2", new IP2Handler()), 
			Map.entry("O", new OHandler()),
			Map.entry("DP3", new DP3Handler()),
			Map.entry("EMPTY", new EMPTYHandler()));

	// ia ca parametrii punctul si telegrama lui
	//gaseste handleru punctului si ii paseaza telegrama
	public static void route(String point, String telegram) throws Exception {
		TelegramHandler handler = HANDLERS.get(point);
		if (handler == null) {
			log.warn("TelegramHandling: no telegram for point {}", point);
			return;
		}
		handler.handle(telegram);
	}
}