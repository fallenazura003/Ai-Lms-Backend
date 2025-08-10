package com.forsakenecho.learning_management_system.repository;

import com.forsakenecho.learning_management_system.entity.LearningProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LearningProgressRepository extends JpaRepository<LearningProgress, UUID> {
    Optional<LearningProgress> findByStudentIdAndCourseId(UUID studentId, UUID courseId);
    List<LearningProgress> findByStudentId(UUID studentId);
}

