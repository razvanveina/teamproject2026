package ro.demo.usersbackend.repository;

import java.util.List;
import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.demo.usersbackend.entity.Location;
import ro.demo.usersbackend.entity.TransportOrder;
import ro.demo.usersbackend.entity.TransportOrderStatus;

public interface TransportOrderRepository extends JpaRepository<TransportOrder, Long> {
	List<TransportOrder> findByStatusAndSourceNotOrderByIdAsc(TransportOrderStatus status, Location source);
	List<TransportOrder> findByStatusAndSourceOrderByIdAsc(TransportOrderStatus status, Location source);
	List<TransportOrder> findByAssignedToAndStatusInOrderByIdAsc(String assignedTo, Collection<TransportOrderStatus> statuses);
	List<TransportOrder> findByAssignedToIsNullAndStatusInOrderByIdAsc(Collection<TransportOrderStatus> statuses);
}
