package ro.demo.usersbackend.entity;

import java.util.List;

public class RetrievalOrder {
	String orderId;
	RetrievalOrderStatus status;
	List<RetrievalOrderLine> lines;
	Destination destination;
}
