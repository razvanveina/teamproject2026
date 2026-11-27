package ro.demo.usersbackend.entity;

import java.time.LocalDateTime;

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

@Entity
@NamedQueries({ //
		@NamedQuery(name = "TransportOrder.findByStatusAndNotSourceOrderByIdAsc", //
				query = "from TransportOrder o where o.status = :status and not o.source = :source order by o.id asc"),
		@NamedQuery(name = "TransportOrder.findByStatusAndSourceOrderByIdAsc", //
				query = "from TransportOrder o where o.status = :status and o.source = :source order by o.id asc"),
		@NamedQuery(name = "TransportOrder.findByStatusAndAssignedToOrderByIdAsc", //
				query = "from TransportOrder o where o.assignedTo = :assignedTo and o.status in :statuses order by o.id asc"),
		@NamedQuery(name = "TransportOrder.findByStatusAndAssignedToIsNullOrderByIdAsc", //
				query = "from TransportOrder o where o.assignedTo is null and o.status in :statuses order by o.id asc"),
		@NamedQuery(name = "TransportOrder.findActiveByLoadUnit", //
				query = "from TransportOrder o where o.status = :status and o.loadUnit = :loadUnit"), //
		@NamedQuery(name = TransportOrder.FIND_ACTIVE_BY_LOAD_UNIT_AND_TARGET, //
				query = "from TransportOrder t where t.loadUnit = :loadUnit and t.target = :target and t.status = :active"), //
		@NamedQuery(name = TransportOrder.FIND_ACTIVE_TARGET_NAME, //
				query = "select t.target.name from TransportOrder t where t.loadUnit.barcode = :barcode and t.status = :active order by t.id desc"), //
		@NamedQuery(name = TransportOrder.COUNT_IN_LANE_TO_TARGET, //
				query = "select count(t) from TransportOrder t where t.status = :active and t.target.name = :target"
						+ " and t.loadUnit.location.name = :point and t.loadUnit.barcode <> :barcode"), //
		@NamedQuery(name = "TransportOrder.countOpenByTargetName", //
				query = "select count(t) from TransportOrder t where t.target.name = :target and t.status in :open") })
public class TransportOrder {
	public static final String FIND_BY_STATUS_AND_NOT_SOURCE_ODERED_BY_ID = "TransportOrder.findByStatusAndNotSourceOrderByIdAsc";
	public static final String FIND_BY_STATUS_AND_SOURCE_ODERED_BY_ID = "TransportOrder.findByStatusAndSourceOrderByIdAsc";
	public static final String FIND_BY_STATUS_AND_ASSIGNED_TO_ODERED_BY_ID = "TransportOrder.findByStatusAndAssignedToOrderByIdAsc";
	public static final String FIND_BY_STATUS_AND_ASSIGNED_TO_IS_NULL_ODERED_BY_ID = "TransportOrder.findByStatusAndAssignedToIsNullOrderByIdAsc";
	public static final String FIND_ACTIVE_BY_LOAD_UNIT = "TransportOrder.findActiveByLoadUnit";
	public static final String FIND_ACTIVE_BY_LOAD_UNIT_AND_TARGET = "TransportOrder.findActiveByLoadUnitAndTarget";
	public static final String FIND_ACTIVE_TARGET_NAME = "TransportOrder.findActiveTargetName";
	public static final String COUNT_IN_LANE_TO_TARGET = "TransportOrder.countInLaneToTarget";
	public static final String COUNT_OPEN_BY_TARGET_NAME = "TransportOrder.countOpenByTargetName";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 50)
	private String assignedTo;

	@ManyToOne
	@JoinColumn(name = "source_id", nullable = false)
	private Location source;

	@ManyToOne
	@JoinColumn(name = "target_id", nullable = false)
	private Location target;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private TransportOrderStatus status;

	@Enumerated(EnumType.STRING)
	private TransportOrderErrorReason errorReason;

	@ManyToOne
	@JoinColumn(name = "loadUnit_id", nullable = false)
	private LoadUnit loadUnit;

	private LocalDateTime createdAt;

	private LocalDateTime modifiedAt;

	private String createdBy;

	private String modifiedBy;

	public TransportOrder() {
		super();
	}

	public TransportOrder(Location source, Location target, TransportOrderStatus status, LoadUnit loadUnit) {
		super();
		this.source = source;
		this.target = target;
		this.status = status;
		this.loadUnit = loadUnit;

		createdAt = LocalDateTime.now();
		createdBy = getCallerName();
	}

	public Location getSource() {
		return source;
	}

	public void setSource(Location source) {
		this.source = source;
		setModifiedBy();
	}

	public Location getTarget() {
		return target;
	}

	public void setTarget(Location target) {
		this.target = target;
		setModifiedBy();
	}

	public TransportOrderStatus getStatus() {
		return status;
	}

	public void setStatus(TransportOrderStatus status) {
		this.status = status;
		setModifiedBy();
	}

	public TransportOrderErrorReason getErrorReason() {
		return errorReason;
	}

	public void setErrorReason(TransportOrderErrorReason errorReason) {
		this.errorReason = errorReason;
		setModifiedBy();
	}

	public LoadUnit getLoadUnit() {
		return loadUnit;
	}

	public void setLoadUnit(LoadUnit loadUnit) {
		this.loadUnit = loadUnit;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
		setModifiedBy();
	}

	public String getAssignedTo() {
		return assignedTo;
	}

	public void setAssignedTo(String assignedTo) {
		this.assignedTo = assignedTo;
		setModifiedBy();
	}

	private void setModifiedBy() {
		modifiedAt = LocalDateTime.now();
		modifiedBy = getCallerName();
	}

	private String getCallerName() {
		return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
				.walk(frames -> frames.skip(1).findFirst().map(frame -> {
					String name = frame.getDeclaringClass().getName();
					int dot = name.lastIndexOf('.');
					int dollar = name.indexOf('$');

					if (dot < 0) {
						if (dollar < 0) {
							return name; // if . and $ do not exist
						} else {
							return name.substring(0, dollar); // if $ exists
						}
					} else {
						if (dollar < 0) {
							return name.substring(dot + 1); // if . exists
						} else {
							return name.substring(dot + 1, dollar); // if both exit
						}
					}
				}).orElse("Unknown"));
	}
}