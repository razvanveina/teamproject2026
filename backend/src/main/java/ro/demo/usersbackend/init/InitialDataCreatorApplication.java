package ro.demo.usersbackend.init;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

import ro.demo.usersbackend.entity.Location;

@SpringBootApplication
@EntityScan(basePackageClasses = Location.class)
public class InitialDataCreatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(InitialDataCreatorApplication.class, args);
	}
}
