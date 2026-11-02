package ro.demo.usersbackend.server.process;

public abstract class AbstractProcess {
	public boolean stop;
	private String name;

	public AbstractProcess(String name) {
		this.name = name;
	}

	public void start() {
		while (true) {
			doAction();

			if (stop) {
				break;
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
