package ro.demo.usersbackend.entity;

import java.util.List;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@NamedQueries({ //
		@NamedQuery(name = "RetrievalOrder.findByOrderId", //
				query = "from RetrievalOrder o where o.orderId = :id"), //
		@NamedQuery(name = "RetrievalOrder.findByStatus", //
				query = "from RetrievalOrder o where o.status = :status order by o.id"), //
		@NamedQuery(name = "RetrievalOrder.findManualByStatus", //
				query = "from RetrievalOrder o where o.status = :status and o.automatic = false order by o.id"), //
		@NamedQuery(name = "RetrievalOrder.findByPickedBy", //
				query = "from RetrievalOrder o where o.pickedBy = :pickedBy"), //
		@NamedQuery(name = "RetrievalOrder.findOrderIdsLike", //
				query = "select o.orderId from RetrievalOrder o where o.orderId like :pattern"), //
		@NamedQuery(name = "RetrievalOrder.findOrderIdsByStatus", //
				query = "select o.orderId from RetrievalOrder o where o.status = :status order by o.id") //
})
@Table(name = "RETRIEVAL_ORDER")
public class RetrievalOrder {
	public static final String BY_ORDER_ID = "RetrievalOrder.findByOrderId";
	public static final String BY_STATUS = "RetrievalOrder.findByStatus";
	public static final String MANUAL_BY_STATUS = "RetrievalOrder.findManualByStatus";
	public static final String BY_PICKED_BY = "RetrievalOrder.findByPickedBy";
	public static final String ORDER_IDS_BY_STATUS = "RetrievalOrder.findOrderIdsByStatus";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String orderId;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private RetrievalOrderStatus status;

	@Column(nullable = false)
	@OneToMany(mappedBy = "order")
	private List<RetrievalOrderLine> lines;

	@ManyToOne
	@JoinColumn(name = "destination_id", nullable = false)
	private Destination destination;

	@ManyToOne
	@JoinColumn(name = "pickingLocation_id")
	private Location pickingLocation;

	@Column(nullable = false)
	private boolean automatic;

	@Column(length = 50)
	private String shippingUser;

	@Column(length = 50)
	private String pickedBy;

	public RetrievalOrder() {
		super();
	}

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public RetrievalOrderStatus getStatus() {
		return status;
	}

	public void setStatus(RetrievalOrderStatus status) {
		this.status = status;
	}

	public List<RetrievalOrderLine> getLines() {
		return lines;
	}

	public void setLines(List<RetrievalOrderLine> lines) {
		this.lines = lines;
	}

	public Destination getDestination() {
		return destination;
	}

	public void setDestination(Destination destination) {
		this.destination = destination;
	}

	public boolean isAutomatic() {
		return automatic;
	}

	public void setAutomatic(boolean automatic) {
		this.automatic = automatic;
	}

	public String getShippingUser() {
		return shippingUser;
	}

	public void setShippingUser(String shippingUser) {
		this.shippingUser = shippingUser;
	}

	public Long getId() {
		return id;
	}

	public String getPickedBy() {
		return pickedBy;
	}

	public void setPickedBy(String pickedBy) {
		this.pickedBy = pickedBy;
	}

	public Location getPickingLocation() {
		return pickingLocation;
	}

	public void setPickingLocation(Location pickingLocation) {
		this.pickingLocation = pickingLocation;
	}
}
