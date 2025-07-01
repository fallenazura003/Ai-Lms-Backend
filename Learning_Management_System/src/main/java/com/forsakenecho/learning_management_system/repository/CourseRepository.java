package com.forsakenecho.learning_management_system.repository;

import com.forsakenecho.learning_management_system.entity.Course;

import org.springframework.data.domain.Page; // Đảm bảo đã import Page
import org.springframework.data.domain.Pageable; // Đảm bảo đã import Pageable
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {
    // ✅ CHỈNH SỬA TỪ List<Course> SANG Page<Course>
    Page<Course> findByVisibleTrue(Pageable pageable);

    Page<Course> findByVisibleTrueAndIdNotIn(List<UUID> excludedIds, Pageable pageable);

    Page<Course> findByCreatorId(UUID userId, Pageable pageable);
}