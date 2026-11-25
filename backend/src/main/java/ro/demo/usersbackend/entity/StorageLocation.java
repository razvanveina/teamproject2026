package ro.demo.usersbackend.entity;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import ro.demo.usersbackend.common.Constants;

@Entity
@DiscriminatorValue("STORAGE")
@NamedQueries({ //
		@NamedQuery(name = "StorageLocation.findClosestFreeSlot", //
				query = "select l from StorageLocation l "
						+ "where not exists (select u from LoadUnit u where u.location = l) " + "and l.aisle > 0 "
						+ "and l.id not in (select t.target.id from TransportOrder t where t.status in ?1) "
						+ "order by l.x asc"),
		@NamedQuery(name = "StorageLocation.findByName", //
				query = "select l from StorageLocation l where l.name = :name") })
public class StorageLocation extends Location {

	public static final String FIND_CLOSEST_FREE_SLOT = "StorageLocation.findClosestFreeSlot";
	public static final String FIND_BY_NAME = "StorageLocation.findByName";

	private int x;

	private int y;

	private int z;

	private int aisle;

	public StorageLocation() {
		super();
	}

	public StorageLocation(int x, int y, int z, int aisle, Area area) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.aisle = aisle;
		this.area = area;
		setName(String.format(Constants.HBW_NAME, aisle, x, y, z));
	}

	public StorageLocation(int x, int y, int z, int aisle) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.aisle = aisle;
		setName(String.format(Constants.HBW_NAME, aisle, x, y, z));
	}

	public StorageLocation(int x, int y, Area area) {
		this.x = x;
		this.y = y;
		this.z = 0;
		this.aisle = 0;
		this.area = area;
		setName(String.format(Constants.PICK_NAME, x, y));
	}

	public StorageLocation(int x, int y) {
		this.x = x;
		this.y = y;
		this.z = 0;
		this.aisle = 0;
		setName(String.format(Constants.PICK_NAME, x, y));
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getZ() {
		return z;
	}

	public void setZ(int z) {
		this.z = z;
	}

	public int getAisle() {
		return aisle;
	}

	public void setAisle(int aisle) {
		this.aisle = aisle;
	}
}