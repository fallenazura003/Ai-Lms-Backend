package com.forsakenecho.learning_management_system.controller;

import com.forsakenecho.learning_management_system.dto.GenerateLessonResponse;
import com.forsakenecho.learning_management_system.service.AiLessonGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/teacher/ai") // Đường dẫn gốc mới cho các API AI của giáo viên
@RequiredArgsConstructor
public class TeacherAiController {

    private final AiLessonGeneratorService aiLessonGeneratorService;

    @PreAuthorize("hasRole('TEACHER')")
    @PostMapping("/generate-lesson") // Đường dẫn cụ thể cho việc tạo bài học
    public Mono<ResponseEntity<GenerateLessonResponse>> generateLessonFromIdea(@RequestBody Map<String, String> request) {
        String lessonIdea = request.get("lessonIdea");
        String courseTitle = request.get("courseTitle");

        if (lessonIdea == null || lessonIdea.trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body(null));
        }

        String actualCourseTitle = (courseTitle != null && !courseTitle.trim().isEmpty()) ? courseTitle : "General Course";

        return aiLessonGeneratorService.generateLesson(actualCourseTitle, lessonIdea)
                .map(response -> ResponseEntity.ok(response))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}