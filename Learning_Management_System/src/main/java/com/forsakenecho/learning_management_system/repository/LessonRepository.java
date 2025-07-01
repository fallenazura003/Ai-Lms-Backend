package com.forsakenecho.learning_management_system.repository;

import com.forsakenecho.learning_management_system.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {
    List<Lesson> findByCourseId(UUID courseId);

    List<Lesson> findByCourseIdOrderByLessonOrderAsc(UUID courseId);
}
