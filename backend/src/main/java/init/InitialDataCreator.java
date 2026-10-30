package init;

import java.util.Locale;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import ro.demo.usersbackend.entity.StorageLocation;

@Component
public class InitialDataCreator implements CommandLineRunner {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	@Transactional
	public void run(String... args) {
		createStorageLocations();
		createTransitLocations();
		createPickingLocations();
	}

	private void createStorageLocations() {
		for (int aisle = 1; aisle <= 5; aisle++) {
			for (int x = 1; x <= 100; x++) {
				for (int y = 1; y <= 5; y++) {
					for (int z = 1; z <= 2; z++) {
						StorageLocation location = new StorageLocation();
						location.setX(x);
						location.setY(y);
						location.setZ(z);
						location.setAisle(aisle);
						location.setName(String.format(Locale.ROOT, "HBW%02d%03d%02d%02d", aisle, x, y, z));
						entityManager.persist(location);
					}
				}
			}
		}
	}

	private void createTransitLocations() {
		persistTransitLocation("IN");

		for (int gate = 1; gate <= 10; gate++) {
			persistTransitLocation(String.format(Locale.ROOT, "GATE-%02d", gate));
		}
	}

	private void createPickingLocations() {
		for (int x = 1; x <= 200; x++) {
			for (int y = 1; y <= 5; y++) {
				persistTransitLocation(String.format(Locale.ROOT, "PICKING-%03d-%02d", x, y));
			}
		}
	}

	private void persistTransitLocation(String name) {
		entityManager.createNativeQuery(
				"INSERT INTO LOCATION (NAME, X, Y, Z, AISLE, DTYPE) "
						+ "VALUES (:name, -1, -1, -1, -1, 'TRANSIT')")
				.setParameter("name", name)
				.executeUpdate();
	}
}
