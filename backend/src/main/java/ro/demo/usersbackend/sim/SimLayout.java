package ro.demo.usersbackend.sim;

import java.io.Serializable;
import java.util.List;

public record SimLayout(List<ConveyorLayout> conveyorLayouts, List<PointLayout> pointLayouts) implements Serializable {
}
