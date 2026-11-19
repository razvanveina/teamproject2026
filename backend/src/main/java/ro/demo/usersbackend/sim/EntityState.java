package ro.demo.usersbackend.sim;

import java.io.Serializable;
import java.util.List;

public record EntityState(String name, List<SimLoadUnit> loadUnits, Long transportOrderId, boolean ready)
		implements Serializable {
}
