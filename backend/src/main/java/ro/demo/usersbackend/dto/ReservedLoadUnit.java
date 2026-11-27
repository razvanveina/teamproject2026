package ro.demo.usersbackend.dto;

// load unit-ul din care s-a rezervat marfa pentru o linie, cu locatia lui curenta
public record ReservedLoadUnit(String barcode, String location) {
}
