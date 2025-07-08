package com.forsakenecho.learning_management_system.repository;

import com.forsakenecho.learning_management_system.entity.Course;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable; // Đảm bảo đã import Pageable
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {
    Page<Course> findByVisibleTrue(Pageable pageable);

    Page<Course> findByVisibleTrueAndIdNotIn(List<UUID> excludedIds, Pageable pageable);

    Page<Course> findByCreatorId(UUID userId, Pageable pageable);

    @Query("""
    SELECT c FROM Course c
    WHERE c.visible = true AND (
        LOWER(c.title) LIKE %:keyword% OR
        LOWER(c.description) LIKE %:keyword% OR
        LOWER(c.creator.name) LIKE %:keyword%
    )
    """)
        // ✅ THAY ĐỔI TỪ List<Course> SANG Page<Course> và THÊM THAM SỐ Pageable
    Page<Course> findVisibleCoursesByKeyword(@Param("keyword") String keyword, Pageable pageable);
}