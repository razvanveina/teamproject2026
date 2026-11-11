package ro.demo.usersbackend.server.mfs;

public class PTTT1Handler extends PointHandler{
	public PTTT1Handler() {
		super("PTTT1");
	}
	
	@Override
	public void handle(String telegram) throws Exception {
		super.handle(telegram);
		StationControl.release("PTTT1");
	}
}