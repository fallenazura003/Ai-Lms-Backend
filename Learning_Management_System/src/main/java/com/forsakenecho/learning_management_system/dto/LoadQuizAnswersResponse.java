package com.forsakenecho.learning_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

// DTO chứa danh sách câu trả lời cho một bài học
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadQuizAnswersResponse {
    private UUID lessonId;
    private List<QuizAnswerResponse> answers;
}