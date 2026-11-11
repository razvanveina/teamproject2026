package ro.demo.usersbackend.server.mfs;

public class PTTS1Handler extends PointHandler{
	public PTTS1Handler() {
		super("PTTS1");
	}
	
	@Override
	public void handle(String telegram) throws Exception {
		super.handle(telegram);
		StationControl.release("PTTS1");
	}
}