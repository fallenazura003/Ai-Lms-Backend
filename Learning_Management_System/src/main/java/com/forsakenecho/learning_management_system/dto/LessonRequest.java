package com.forsakenecho.learning_management_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonRequest {
    @NotBlank(message = "Tiêu đề bài học không được để trống")
    private String title;
    private String youtubeVideoId;

    private String recallQuestion;
    private String material;
    private String shortAnswer;
    private String multipleChoice;
    private String summaryTask;

    private boolean isRecallQuestionCompleted;
    private boolean isMaterialCompleted;
    private boolean isShortAnswerCompleted;
    private boolean isMultipleChoiceCompleted;
    private boolean isSummaryTaskCompleted;
    private boolean isLessonCompleted;


    @NotNull(message = "Thứ tự bài học không được để trống") // ✅ Thêm lessonOrder
    private Integer lessonOrder;
}
