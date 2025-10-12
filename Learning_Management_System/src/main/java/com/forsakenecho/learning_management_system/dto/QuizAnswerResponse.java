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
public class QuizAnswerResponse {
    private UUID id;
    private String questionId;
    private String selectedOptionId;
    private UUID lessonId;
    private UUID userId; // Giả sử Frontend cần biết ID người dùng
}