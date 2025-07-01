package com.forsakenecho.learning_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonResponse {
    private UUID id;
    private String title;
    private String youtubeVideoId;

    // Các trường nội dung chi tiết
    private String recallQuestion;
    private String material;
    private String shortAnswer;
    private String multipleChoice;
    private String summaryTask;

    // Các trường trạng thái hoàn thành
    private boolean isRecallQuestionCompleted;
    private boolean isMaterialCompleted;
    private boolean isShortAnswerCompleted;
    private boolean isMultipleChoiceCompleted;
    private boolean isSummaryTaskCompleted;
    private boolean isLessonCompleted; // Trạng thái hoàn thành tổng thể
    private Integer lessonOrder;
    private UUID courseId; // ID của khóa học mà bài học này thuộc về
}