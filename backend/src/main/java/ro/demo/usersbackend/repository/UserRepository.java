
package ro.demo.usersbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.demo.usersbackend.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
