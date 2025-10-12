package com.forsakenecho.learning_management_system.service;

import com.forsakenecho.learning_management_system.dto.LoadQuizAnswersResponse;
import com.forsakenecho.learning_management_system.dto.QuizAnswerResponse;
import com.forsakenecho.learning_management_system.dto.SaveQuizAnswerRequest;
import com.forsakenecho.learning_management_system.entity.Lesson;
import com.forsakenecho.learning_management_system.entity.QuizAnswer;
import com.forsakenecho.learning_management_system.entity.User;
import com.forsakenecho.learning_management_system.repository.LessonRepository;
import com.forsakenecho.learning_management_system.repository.QuizAnswerRepository;
import com.forsakenecho.learning_management_system.repository.UserRepository; // Giả sử có UserRepository
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizAnswerService {
    private final QuizAnswerRepository quizAnswerRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository; // Cần thiết để lấy đối tượng User từ ID

    private QuizAnswerResponse mapToResponse(QuizAnswer answer) {
        return QuizAnswerResponse.builder()
                .id(answer.getId())
                .questionId(answer.getQuestionId())
                .selectedOptionId(answer.getSelectedOptionId())
                .lessonId(answer.getLesson().getId())
                .userId(answer.getUser().getId())
                .build();
    }

    /**
     * Tải tất cả các câu trả lời đã lưu của người dùng cho một bài học.
     */
    public LoadQuizAnswersResponse loadAnswers(UUID lessonId, UUID userId) {
        // 1. Kiểm tra sự tồn tại của Lesson và User (Tùy chọn, nếu cần xác thực nghiêm ngặt)
        if (!lessonRepository.existsById(lessonId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found with ID: " + lessonId);
        }

        List<QuizAnswer> answers = quizAnswerRepository.findByUserIdAndLessonId(userId, lessonId);

        List<QuizAnswerResponse> responseList = answers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return LoadQuizAnswersResponse.builder()
                .lessonId(lessonId)
                .answers(responseList)
                .build();
    }

    /**
     * Lưu hoặc cập nhật câu trả lời của người dùng.
     */
    @Transactional
    public QuizAnswerResponse saveOrUpdateAnswer(UUID lessonId, UUID userId, SaveQuizAnswerRequest request) {
        // 1. Tìm Lesson và User
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found with ID: " + lessonId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with ID: " + userId));

        // 2. Kiểm tra câu trả lời đã tồn tại chưa
        Optional<QuizAnswer> existingAnswerOpt = quizAnswerRepository.findByUserIdAndLessonIdAndQuestionId(
                userId, lessonId, request.getQuestionId());

        QuizAnswer answer;

        if (existingAnswerOpt.isPresent()) {
            // Cập nhật câu trả lời hiện có
            answer = existingAnswerOpt.get();
            answer.setSelectedOptionId(request.getSelectedOptionId());
            answer.setUpdatedAt(LocalDateTime.now());
        } else {
            // Tạo mới câu trả lời
            answer = QuizAnswer.builder()
                    .user(user)
                    .lesson(lesson)
                    .questionId(request.getQuestionId())
                    .selectedOptionId(request.getSelectedOptionId())
                    .build();
        }

        // 3. Lưu vào DB
        QuizAnswer savedAnswer = quizAnswerRepository.save(answer);
        return mapToResponse(savedAnswer);
    }
}
