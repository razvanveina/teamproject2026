package ro.demo.usersbackend.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
@DiscriminatorValue("STORAGE")
public class StorageLocation extends Location {
	private int x;

	private int y;

	private int z;

	private int aisle;

	@OneToOne
	@JoinColumn(name = "loadUnit_id")
	private LoadUnit loadUnit;

	public StorageLocation() {
		super();
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

	public LoadUnit getLoadUnit() {
		return loadUnit;
	}

	public void setLoadUnit(LoadUnit loadUnit) {
		this.loadUnit = loadUnit;
	}
}
