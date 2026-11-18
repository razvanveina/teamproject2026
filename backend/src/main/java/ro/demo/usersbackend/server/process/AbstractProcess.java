package ro.demo.usersbackend.server.process;

import ro.demo.usersbackend.common.Constants;

public abstract class AbstractProcess {
	public volatile boolean stop;
	public volatile boolean paused;
	private String name;

	public AbstractProcess(String name) {
		this.name = name;
	}

	public synchronized void start() {
		while (!stop) {
			if (!paused) {
				doAction();
			}

			try {
				Thread.sleep(Constants.PROCESS_INTERVAL);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
			}
		}
	}

	public void stop() {
		this.stop = true;
	}

	protected abstract void doAction();

	public String getName() {
		return name;
	}

}