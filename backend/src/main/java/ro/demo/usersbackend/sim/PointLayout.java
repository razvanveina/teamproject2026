package ro.demo.usersbackend.sim;

import java.io.Serializable;

public record PointLayout(String name, int x, int y) implements Serializable {
}
