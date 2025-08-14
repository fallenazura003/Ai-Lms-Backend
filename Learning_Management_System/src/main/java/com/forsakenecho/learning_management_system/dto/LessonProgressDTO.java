package com.forsakenecho.learning_management_system.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class LessonProgressDTO {
    private UUID lessonId;
    private boolean completed;

    public LessonProgressDTO(UUID lessonId, boolean completed) {
        this.lessonId = lessonId;
        this.completed = completed;
    }

    public UUID getLessonId() {
        return lessonId;
    }

    public boolean isCompleted() {
        return completed;
    }
}
