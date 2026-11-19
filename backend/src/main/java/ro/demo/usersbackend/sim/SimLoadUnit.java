package ro.demo.usersbackend.sim;

import java.io.Serializable;

public record SimLoadUnit(String barcode, String type) implements Serializable {
}
