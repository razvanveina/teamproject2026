package ro.demo.usersbackend.service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.springframework.stereotype.Service;

import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.entity.Workplace;

@Service
public class WorkplaceService {
	private static final Logger log = LogManager.getLogger("application");

	public String findNameFor(String remoteAddress) {
		InetAddress client;
		try {
			client = InetAddress.getByName(remoteAddress);
		} catch (UnknownHostException e) {
			log.warn("Workplace: cannot read the client address {}", remoteAddress);
			return null;
		}

		List<Workplace> workplaces = new WithSessionAndTransaction<List<Workplace>>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				setReturnValue(session.createQuery("from Workplace w order by w.id", Workplace.class).getResultList());
			}
		}.run();

		for (Workplace workplace : workplaces) {
			if (matches(workplace, client)) {
				return workplace.getName();
			}
		}
		return null;
	}

	private boolean matches(Workplace workplace, InetAddress client) {
		try {
			for (InetAddress address : InetAddress.getAllByName(workplace.getIp().trim())) {
				if (address.equals(client) || (address.isLoopbackAddress() && client.isLoopbackAddress())) {
					return true;
				}
			}
		} catch (UnknownHostException e) {
			log.warn("Workplace {}: host {} not found", workplace.getName(), workplace.getIp());
		}
		return false;
	}
}
