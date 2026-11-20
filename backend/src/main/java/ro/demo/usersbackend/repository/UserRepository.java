
package ro.demo.usersbackend.repository;

import org.hibernate.Session;

import ro.demo.usersbackend.entity.User;

public class UserRepository extends BaseRepository<User, Long> {

	public UserRepository(Session session) {
		super(session, User.class);
	}

<<<<<<< HEAD
}
=======
}
>>>>>>> 0a243cfd365abc70c9808d6f2d60725b6f74f219
