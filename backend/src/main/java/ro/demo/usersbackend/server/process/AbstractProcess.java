package ro.demo.usersbackend.server.process;

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
				Thread.sleep(1000);
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
