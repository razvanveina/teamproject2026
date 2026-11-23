package ro.demo.usersbackend.service;

import java.util.List;

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
				setReturnValue(new UserRepository(session).findAll());
			}
		}.run();
	}

	public User findById(Long id) {
		return new WithSessionAndTransaction<User>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				setReturnValue(find(id, session));
			}
		}.run();
	}

	public User create(User user) {
		return new WithSessionAndTransaction<User>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				setReturnValue(new UserRepository(session).save(user));
			}
		}.run();
	}

	public User update(Long id, User updatedUser) {
		return new WithSessionAndTransaction<User>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				User existingUser = find(id, session);
				existingUser.setName(updatedUser.getName());
				existingUser.setEmail(updatedUser.getEmail());
				setReturnValue(existingUser);
			}
		}.run();
	}

	public void delete(Long id) {
		new WithSessionAndTransaction<>() {
			@Override
			protected void executeBusinessLogic(Session session) {
				new UserRepository(session).delete(find(id, session));
			}
		}.run();
	}

	private User find(Long id, Session session) {
		return new UserRepository(session).findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id));
	}
}