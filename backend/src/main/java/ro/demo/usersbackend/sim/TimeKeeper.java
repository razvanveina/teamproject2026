package ro.demo.usersbackend.sim;

public class TimeKeeper {
	private static TimeKeeper instance;
	
	private boolean paused;
	private long pausedAt;
	private long pausedTotal;

	private TimeKeeper() {
		paused = false;
		pausedAt = 0;
		pausedTotal = 0;
	}
	
	public static TimeKeeper getInstance() {
		if(instance == null) {
			instance = new TimeKeeper();
		}
		return instance;
	}
	
	public  long currentSimulationTime() {
		return (paused ? pausedAt : System.currentTimeMillis()) - pausedTotal;
	}

	public void togglePause() {
		long t = System.currentTimeMillis();
		if (paused) {
			pausedTotal += t - pausedAt;
		} else {
			pausedAt = t;
		}
		paused = !paused;
	}

	public boolean isPaused() {
		return paused;
	}
}
