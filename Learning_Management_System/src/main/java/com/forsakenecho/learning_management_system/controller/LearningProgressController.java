package com.forsakenecho.learning_management_system.controller;

import com.forsakenecho.learning_management_system.entity.LearningProgress;
import com.forsakenecho.learning_management_system.entity.User;
import com.forsakenecho.learning_management_system.service.LearningProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/student/progress")
@RequiredArgsConstructor

public class LearningProgressController {
    private final LearningProgressService progressService;

    // Endpoint để đánh dấu một bài học đã hoàn thành
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/complete-lesson")
    public ResponseEntity<LearningProgress> completeLesson(
            @RequestParam UUID courseId,
            @RequestParam UUID lessonId,
            Authentication authentication
    ) {
        User student = (User) authentication.getPrincipal();
        LearningProgress updated = progressService.completeLesson(student.getId(), courseId, lessonId);
        return ResponseEntity.ok(updated);
    }

    // Endpoint cũ: Lấy toàn bộ tiến độ của tất cả các khóa học
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping
    public ResponseEntity<List<LearningProgress>> getProgress(Authentication authentication) {
        User student = (User) authentication.getPrincipal();
        return ResponseEntity.ok(progressService.getProgressForStudent(student.getId()));
    }

    // ✅ Endpoint mới: Lấy tiến độ của một khóa học cụ thể
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/{courseId}")
    public ResponseEntity<LearningProgress> getProgressForCourse(
            @PathVariable UUID courseId,
            Authentication authentication
    ) {
        User student = (User) authentication.getPrincipal();
        return progressService.getProgressForStudentAndCourse(student.getId(), courseId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
