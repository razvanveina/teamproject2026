package ro.demo.usersbackend.sim;

import java.io.Serializable;

public record ConveyorLayout(String name, int[] path, int capacity) implements Serializable {
}
