
package ro.demo.usersbackend.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ro.demo.usersbackend.entity.User;
import ro.demo.usersbackend.repository.UserRepository;

@Service
@Transactional
public class UserService {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public List<User> findAll() {
		return userRepository.findAll();
	}

	@Transactional(readOnly = true)
	public User findById(Long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id));
	}

	public User create(User user) {
		return userRepository.save(user);
	}

	public User update(Long id, User updatedUser) {
		User existingUser = findById(id);

		existingUser.setName(updatedUser.getName());
		existingUser.setEmail(updatedUser.getEmail());

		return userRepository.save(existingUser);
	}

	public void delete(Long id) {
		User user = findById(id);
		userRepository.delete(user);
	}
}
