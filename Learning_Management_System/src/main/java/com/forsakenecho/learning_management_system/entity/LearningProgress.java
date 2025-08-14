package com.forsakenecho.learning_management_system.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "learning_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "course_id"}))
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ToString.Exclude
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnore // Sửa lỗi tuần tự hóa
    @ToString.Exclude
    private Course course;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "completed_lessons", joinColumns = @JoinColumn(name = "progress_id"))
    @Column(name = "lesson_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    private Set<UUID> completedLessonIds = new HashSet<>();

    private int completedLessons;
    private int totalLessons; // Giá trị này sẽ được Service set

    private LocalDateTime lastAccessedAt;

    @Enumerated(EnumType.STRING)
    private ProgressStatus status;

    public enum ProgressStatus { IN_PROGRESS, COMPLETED }

    @PrePersist
    @PreUpdate
    private void syncCounts() {
        this.completedLessons = this.completedLessonIds != null ? this.completedLessonIds.size() : 0;
        this.status = (totalLessons > 0 && completedLessons >= totalLessons)
                ? ProgressStatus.COMPLETED
                : ProgressStatus.IN_PROGRESS;
    }
}
