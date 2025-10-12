package com.forsakenecho.learning_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.util.List;

// DTO cho việc lưu câu trả lời từ Frontend
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveQuizAnswerRequest {
    // ID của câu hỏi trong nội dung JSON của Lesson
    private String questionId;

    // ID của lựa chọn mà học sinh chọn
    private String selectedOptionId;

}





