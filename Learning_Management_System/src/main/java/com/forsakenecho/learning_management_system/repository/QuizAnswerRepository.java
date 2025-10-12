package com.forsakenecho.learning_management_system.repository;

import com.forsakenecho.learning_management_system.entity.QuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, UUID> {

    /**
     * Tìm tất cả câu trả lời của một người dùng cho một bài học cụ thể.
     */
    List<QuizAnswer> findByUserIdAndLessonId(UUID userId, UUID lessonId);

    /**
     * Tìm một câu trả lời cụ thể của một người dùng cho một câu hỏi trong một bài học.
     * Dùng để cập nhật (UPDATE) câu trả lời.
     */
    Optional<QuizAnswer> findByUserIdAndLessonIdAndQuestionId(UUID userId, UUID lessonId, String questionId);
}
