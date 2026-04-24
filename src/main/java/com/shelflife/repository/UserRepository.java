package com.shelflife.repository;

import com.shelflife.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
	Optional<User> findByEmail(String email);

	Optional<User> findByEmailNormalized(String emailNormalized);
}
