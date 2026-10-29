package ro.demo.usersbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("STORAGE")
public class StorageLocation extends Location {

	@Column(name = "X_COORDINATE")
	private Integer x;

	@Column(name = "Y_COORDINATE")
	private Integer y;

	@Column(name = "Z_COORDINATE")
	private Integer z;

	@Column(name = "AISLE")
	private String aisle;

	private LoadUnit loadUnit;

	public StorageLocation() {
	}

	public StorageLocation(Integer x, Integer y, Integer z, String aisle) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.aisle = aisle;
	}

	public Integer getX() {
		return x;
	}

	public void setX(Integer x) {
		this.x = x;
	}

	public Integer getY() {
		return y;
	}

	public void setY(Integer y) {
		this.y = y;
	}

	public Integer getZ() {
		return z;
	}

	public void setZ(Integer z) {
		this.z = z;
	}

	public String getAisle() {
		return aisle;
	}

	public void setAisle(String aisle) {
		this.aisle = aisle;
	}
}
