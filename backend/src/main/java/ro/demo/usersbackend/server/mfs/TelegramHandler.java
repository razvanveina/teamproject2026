package ro.demo.usersbackend.server.mfs;

public interface TelegramHandler {
	void handle(String telegram) throws Exception;
}
