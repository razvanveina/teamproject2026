
package ro.demo.usersbackend.repository;

import org.hibernate.Session;

import ro.demo.usersbackend.entity.User;

public class UserRepository extends BaseRepository<User, Long> {

	public UserRepository(Session session) {
		super(session, User.class);
	}
}
