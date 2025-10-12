package com.forsakenecho.learning_management_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import java.sql.Types;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class QuizAnswer {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "CHAR(36)")
    @JdbcTypeCode(Types.VARCHAR)
    private UUID id;

    // Liên kết với User (Học sinh)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Liên kết với Lesson (Bài học chứa câu hỏi này)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    // ID của câu hỏi (questionId). Vì nội dung câu hỏi là một JSON lớn trong Lesson.multipleChoice,
    // ta cần một ID duy nhất để xác định câu hỏi nào đang được trả lời.
    // Giả sử Frontend sẽ tạo ra ID này (ví dụ: Q1, Q2, Q3) khi phân tích JSON
    @Column(nullable = false)
    private String questionId;

    // ID của lựa chọn được học sinh chọn (optionId). Ví dụ: A, B, C, D
    @Column(nullable = false)
    private String selectedOptionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
