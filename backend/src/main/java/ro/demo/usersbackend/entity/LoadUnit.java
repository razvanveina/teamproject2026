package ro.demo.usersbackend.entity;

import java.util.List;

public class LoadUnit {
	String barcode;
	LoadUnitType type;
	Location location;
	List<StockItem> stockItems;
}
