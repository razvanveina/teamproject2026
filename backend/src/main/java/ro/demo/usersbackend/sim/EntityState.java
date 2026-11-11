package ro.demo.usersbackend.sim;

import java.io.Serializable;
import java.util.List;

public record EntityState(String name, List<String> loadUnits, Long transportOrderId, boolean ready)
		implements Serializable {
}
