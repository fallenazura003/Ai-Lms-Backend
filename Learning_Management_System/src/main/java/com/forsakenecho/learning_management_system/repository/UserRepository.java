package com.forsakenecho.learning_management_system.repository;

import com.forsakenecho.learning_management_system.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    Page<User> findByRoleInOrderByCreatedAtDesc(List<String> roles, Pageable pageable);
}
