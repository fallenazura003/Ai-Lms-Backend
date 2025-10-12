package com.forsakenecho.learning_management_system.repository;

import com.forsakenecho.learning_management_system.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    Page<User> findByRoleInOrderByCreatedAtDesc(List<String> roles, Pageable pageable);
    // ✅ PHƯƠNG THỨC TÌM KIẾM MỚI CHO ADMIN (Tìm kiếm theo Tên hoặc Email)
    @Query("SELECT u FROM User u WHERE u.role IN :roles AND (LOWER(u.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<User> searchUsersByRoleAndTerm(
            @Param("roles") List<String> roles,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );
}
