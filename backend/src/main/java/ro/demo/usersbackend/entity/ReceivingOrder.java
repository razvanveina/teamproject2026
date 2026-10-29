package ro.demo.usersbackend.entity;

import java.util.List;

public class ReceivingOrder {
	String orderId;
	ReceivingOrderStatus status;
	List<ReceivingOrderLine> lines;
}
