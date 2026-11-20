package ro.demo.usersbackend.repository;

import java.util.List;
import java.util.Optional;

import org.hibernate.Session;

import ro.demo.usersbackend.entity.LoadUnit;
import ro.demo.usersbackend.entity.Location;

public class LoadUnitRepository extends BaseRepository<LoadUnit, Long> {

	public LoadUnitRepository(Session session) {
		super(session, LoadUnit.class);
	}

	public Optional<LoadUnit> findByBarcode(String barcode) {
		return session.createNamedQuery(LoadUnit.BY_BARCODE, LoadUnit.class).setParameter("barcode", barcode)
				.uniqueResultOptional();
	}

	public List<LoadUnit> findByLocationOrderByIdAsc(Location location) {
		return session.createNamedQuery(LoadUnit.BY_LOCATION_ORDER_BY_ID_ASC, LoadUnit.class)
				.setParameter("location", location).getResultList();
	}

	public List<LoadUnit> findByLocationName(String name) {
		return session.createNamedQuery(LoadUnit.BY_LOCATION_NAME, LoadUnit.class).setParameter("name", name)
				.getResultList();
	}
}
