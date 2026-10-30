package ro.demo.usersbackend.init;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;

import ro.demo.usersbackend.entity.Location;

@SpringBootApplication
@EntityScan(basePackageClasses = Location.class)
public class InitialDataCreatorApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(InitialDataCreatorApplication.class, args);

		SpringApplication.exit(context, () -> 0);
	}
}
