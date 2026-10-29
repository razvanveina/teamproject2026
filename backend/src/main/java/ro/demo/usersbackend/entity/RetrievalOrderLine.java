package ro.demo.usersbackend.entity;

import java.util.List;

public class RetrievalOrderLine {
	Article article;
	int quantity;
	RetrievalOrderLineStatus status;

	List<Reservation> reservations;
}
