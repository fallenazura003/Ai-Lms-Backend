package com.forsakenecho.learning_management_system.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GenerateLessonResponse {
    private String title;
    private String youtubeVideoId;
    private String recallQuestion;
    private String material;
    private String shortAnswer;
    private String multipleChoice;
    private String summaryTask;
}