
package ro.demo.usersbackend.service;

import java.util.List;
import java.util.Optional;

import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import ro.demo.usersbackend.core.persistence.WithSessionAndTransaction;
import ro.demo.usersbackend.entity.User;
import ro.demo.usersbackend.repository.UserRepository;

@Service
public class UserService {

	public List<User> findAll() {
		return new WithSessionAndTransaction<List<User>>() {

			@Override
			protected void executeBusinessLogic(Session session) {
				UserRepository userRepository = new UserRepository(session);
				setReturnValue(userRepository.findAll());
			}
		}.run();
	}

	private User findById(Long id, Session session) {
		UserRepository userRepository = new UserRepository(session);
		Optional<User> user = userRepository.findById(id);

		if (user.isPresent()) {
			return user.get();
		} else {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id);
		}
	}

	public User create(User user) {
		return new WithSessionAndTransaction<User>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				UserRepository userRepository = new UserRepository(session);
				setReturnValue(userRepository.save(user));
			}
		}.run();
	}

	public User update(Long id, User updatedUser) {
		return new WithSessionAndTransaction<User>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				UserRepository userRepository = new UserRepository(session);
				User existingUser = findById(id, session);

				existingUser.setName(updatedUser.getName());
				existingUser.setEmail(updatedUser.getEmail());

				setReturnValue(userRepository.save(existingUser));
			}
		}.run();
	}

	public void delete(Long id) {
		new WithSessionAndTransaction<User>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				UserRepository userRepository = new UserRepository(session);
				userRepository.delete(findById(id, session));
			}
		}.run();
	}
}