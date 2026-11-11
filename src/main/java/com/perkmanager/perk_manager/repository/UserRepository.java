package com.perkmanager.perk_manager.repository;

import com.perkmanager.perk_manager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Exact username lookup (usernames are stored lowercase). Optional is safe: username is UNIQUE.
    Optional<User> findByUsername(String username);

    // Used to give a friendly "already taken" message before inserting
    boolean existsByUsername(String username);
}
