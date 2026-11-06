package ro.demo.usersbackend.server.process;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import ro.demo.usersbackend.dto.ProcessResponse;
import ro.demo.usersbackend.server.host.HostReceivingProcess;
import ro.demo.usersbackend.server.host.HostRetrievalProcess;
import ro.demo.usersbackend.server.reservation.ReservationService;
import ro.demo.usersbackend.server.transport.ToteTransportService;

public class ProcessManager {
	private static final AbstractProcess[] processes = { new HostReceivingProcess(), new HostRetrievalProcess(), new ReservationService(), new ToteTransportService() };
	private static final Map<String, Thread> threads = new HashMap<>();

	public static synchronized void startAll() {
		for (AbstractProcess process : processes) {
			start(process.getName());
		}
	}

	public static synchronized List<ProcessResponse> findAll() {
		List<ProcessResponse> result = new ArrayList<>();
		for (AbstractProcess process : processes) {
			result.add(toResponse(process));
		}
		return result;
	}

	private static ProcessResponse toResponse(AbstractProcess process) {
		Thread thread = threads.get(process.getName());
		String status;

		if (thread == null || !thread.isAlive() || process.stop) {
			status = "STOPPED";
		} else if (process.paused) {
			status = "PAUSED";
		} else {
			status = "RUNNING";
		}
		return new ProcessResponse(process.getName(), status);
	}

	// returnam null daca nu gasim procesul cu numele respectiv
	public static synchronized ProcessResponse start(String name) {
		AbstractProcess process = find(name);
		if(process == null) {
			return null;
		}
		
		process.paused = false;
		process.stop = false;
		
		Thread thread = threads.get(process.getName());
		if(thread == null || !thread.isAlive()) {
			thread = new Thread(process::start, name);
			thread.setDaemon(true);
			threads.put(name, thread);
			thread.start();
		}
		return toResponse(process);
	}

	private static AbstractProcess find(String name) {
		for(AbstractProcess process : processes) {
			if(process.getName().equals(name)) {
				return process;
			}
		}
		return null;
	}
	
	public static synchronized ProcessResponse stop(String name) {
		AbstractProcess process = find(name);
		if(process == null) {
			return null;
		}
		
		process.stop();
		return toResponse(process);
	}
	
	public static synchronized ProcessResponse pause(String name) {
		AbstractProcess process = find(name);
		if(process == null) {
			return null;
		}
		
		process.paused = true;
		return toResponse(process);
	}
}