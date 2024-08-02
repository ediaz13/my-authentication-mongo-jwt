package com.mogul.authentication.mongoauthentication.repository;

import com.mogul.authentication.mongoauthentication.document.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
