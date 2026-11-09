package ro.demo.usersbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.demo.usersbackend.entity.Reservation;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
}
