package com.forsakenecho.learning_management_system.repository;

import com.forsakenecho.learning_management_system.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {
    List<Lesson> findByCourseId(UUID courseId);

    List<Lesson> findByCourseIdOrderByLessonOrderAsc(UUID courseId);

    @Query("SELECT l.id FROM Lesson l WHERE l.course.id = :courseId")
    List<UUID> findLessonIdsByCourseId(@Param("courseId") UUID courseId);

    // ✅ Thêm phương thức để đếm số lượng bài học một cách hiệu quả
    int countByCourseId(UUID courseId);
}
