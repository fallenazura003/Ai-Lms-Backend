package com.forsakenecho.learning_management_system.controller;

import com.forsakenecho.learning_management_system.dto.LoadQuizAnswersResponse;
import com.forsakenecho.learning_management_system.dto.QuizAnswerResponse;
import com.forsakenecho.learning_management_system.dto.SaveQuizAnswerRequest;
import com.forsakenecho.learning_management_system.entity.User;
import com.forsakenecho.learning_management_system.service.QuizAnswerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/student/lessons/{lessonId}/quiz")
@RequiredArgsConstructor
public class StudentQuizController {
    private final QuizAnswerService quizAnswerService;

    // Tải câu trả lời đã lưu
    @GetMapping("/answers")
    @PreAuthorize("hasRole('STUDENT')") // Chỉ học sinh mới được xem câu trả lời của mình
    public ResponseEntity<LoadQuizAnswersResponse> loadAnswers(
            @PathVariable UUID lessonId,
            Authentication authentication) {

        // Lấy ID người dùng hiện tại
        User currentUser = (User) authentication.getPrincipal();
        UUID userId = currentUser.getId();

        LoadQuizAnswersResponse response = quizAnswerService.loadAnswers(lessonId, userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // Lưu hoặc cập nhật câu trả lời
    @PostMapping("/answers")
    @PreAuthorize("hasRole('STUDENT')") // Chỉ học sinh mới được lưu câu trả lời
    public ResponseEntity<QuizAnswerResponse> saveAnswer(
            @PathVariable UUID lessonId,
            @Valid @RequestBody SaveQuizAnswerRequest request,
            Authentication authentication) {

        // Lấy ID người dùng hiện tại
        User currentUser = (User) authentication.getPrincipal();
        UUID userId = currentUser.getId();

        QuizAnswerResponse savedAnswer = quizAnswerService.saveOrUpdateAnswer(lessonId, userId, request);
        return new ResponseEntity<>(savedAnswer, HttpStatus.CREATED);
    }
}
