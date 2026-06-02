package com.campusrecycle.repository;

import com.campusrecycle.model.User;
import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Used for the new Login system
    Optional<User> findByEmail(String email);

    // Used to prevent duplicate accounts during Registration
    Boolean existsByEmail(String email);

    // Kept from your previous setup for backward compatibility
    Optional<User> findByGithubId(String githubId);
}