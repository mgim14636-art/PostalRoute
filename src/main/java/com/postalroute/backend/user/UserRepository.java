package com.postalroute.backend.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByNameAndPhoneNumber(String name, String phoneNumber);
    Optional<User> findByNameAndUsernameAndPhoneNumber(String name, String username, String phoneNumber);
}