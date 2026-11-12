package ro.demo.usersbackend.server.mfs;

public class OHandler extends PointHandler {
	public OHandler() {
		super("O");
	}

	@Override
	public void handle(String telegram) throws Exception {
		super.handle(telegram);
		StationControl.release("O");
	}
}
