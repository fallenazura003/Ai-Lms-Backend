package com.forsakenecho.learning_management_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "learning_progress")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningProgress {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "CHAR(36)")
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    // ✅ Thêm @Builder.Default để đảm bảo khởi tạo khi dùng builder
    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "completed_lessons", joinColumns = @JoinColumn(name = "progress_id"))
    @Column(name = "lesson_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    private Set<UUID> completedLessonIds = new HashSet<>();

    // ✅ Số lượng bài học hoàn thành được tính tự động
    private int completedLessons;
    private int totalLessons;

    private LocalDateTime lastAccessedAt;

    @Enumerated(EnumType.STRING)
    private ProgressStatus status;

    public enum ProgressStatus {
        IN_PROGRESS, COMPLETED
    }
}
