package ro.demo.usersbackend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.demo.usersbackend.entity.DestinationToGate;

public interface DestinationToGateRepository extends JpaRepository<DestinationToGate, Long> {

	List<DestinationToGate> findByGateName(String name);

	Optional<DestinationToGate> findByDestinationName(String name);
}