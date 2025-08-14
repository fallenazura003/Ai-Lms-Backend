package com.forsakenecho.learning_management_system.dto;

import com.forsakenecho.learning_management_system.entity.LearningProgress;
import lombok.Builder;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class CourseProgressSummaryDTO {
    private UUID courseId;
    private int completedLessons;
    private int totalLessons;
    private int percent; // 0..100
    private LearningProgress.ProgressStatus status;
    private Set<UUID> completedLessonIds;

    public static CourseProgressSummaryDTO from(LearningProgress lp) {
        int total = Math.max(lp.getTotalLessons(), 0);
        int done = Math.max(lp.getCompletedLessons(), 0);
        int percent = (total > 0) ? Math.round(done * 100f / total) : 0;

        return CourseProgressSummaryDTO.builder()
                .courseId(lp.getCourse().getId())
                .completedLessons(done)
                .totalLessons(total)
                .percent(percent)
                .status(lp.getStatus())
                .completedLessonIds(lp.getCompletedLessonIds())
                .build();
    }
}
