package com.forsakenecho.learning_management_system.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode; // Import này cho @JdbcTypeCode
import java.sql.Types; // Import này cho Types.VARCHAR

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lesson {
    @Id
    @GeneratedValue // Sử dụng GenerationType.UUID hoặc AUTO tùy theo cấu hình Hibernate của bạn
    @Column(columnDefinition = "CHAR(36)") // Đảm bảo kiểu cột phù hợp với UUID
    @JdbcTypeCode(Types.VARCHAR) // Đảm bảo Hibernate map UUID sang VARCHAR/CHAR(36)
    private UUID id;

    @Column(nullable = false)
    private String title;

    private String youtubeVideoId;

    @ManyToOne
    @JoinColumn(name = "course_id")
    @JsonBackReference
    private Course course;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 5 bước nội dung
    @Column(columnDefinition = "LONGTEXT") // Dùng TEXT cho các trường nội dung có thể dài
    private String recallQuestion;
    @Column(columnDefinition = "LONGTEXT")
    private String material;
    @Column(columnDefinition = "LONGTEXT")
    private String shortAnswer;
    @Column(columnDefinition = "LONGTEXT")
    private String multipleChoice;
    @Column(columnDefinition = "LONGTEXT")
    private String summaryTask;

    // Các trường trạng thái hoàn thành cần được thêm vào Entity
    private boolean isRecallQuestionCompleted;
    private boolean isMaterialCompleted;
    private boolean isShortAnswerCompleted;
    private boolean isMultipleChoiceCompleted;
    private boolean isSummaryTaskCompleted;
    private boolean isLessonCompleted; // Trạng thái hoàn thành tổng thể của bài học

    // Thứ tự bài học trong khóa học
    @Column(nullable = false)
    private int lessonOrder;

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