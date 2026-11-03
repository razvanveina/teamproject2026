
package ro.demo.usersbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import ro.demo.usersbackend.init.DemoTestDataCreator;
import ro.demo.usersbackend.init.InitialDataCreator;
import ro.demo.usersbackend.init.InitialDataCreatorApplication;
import ro.demo.usersbackend.server.process.ProcessManager;

@SpringBootApplication
@ComponentScan(excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
		InitialDataCreator.class, InitialDataCreatorApplication.class, DemoTestDataCreator.class }))
public class UsersBackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(UsersBackendApplication.class, args);
		ProcessManager.startAll();
	}
}
