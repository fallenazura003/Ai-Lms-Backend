package com.forsakenecho.learning_management_system.controller;

import com.forsakenecho.learning_management_system.dto.CourseProgressSummaryDTO;
import com.forsakenecho.learning_management_system.dto.LearningProgressDTO;
import com.forsakenecho.learning_management_system.dto.LessonProgressDTO;
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

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/uncomplete-lesson")
    public ResponseEntity<LearningProgress> uncompleteLesson(
            @RequestParam UUID courseId,
            @RequestParam UUID lessonId,
            Authentication authentication
    ) {
        User student = (User) authentication.getPrincipal();
        LearningProgress updated = progressService.uncompleteLesson(student.getId(), courseId, lessonId);
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping
    public ResponseEntity<List<LearningProgressDTO>> getAll(Authentication authentication) {
        User student = (User) authentication.getPrincipal();
        return ResponseEntity.ok(progressService.getProgressForStudent(student.getId()));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/{courseId}")
    public ResponseEntity<LearningProgressDTO> getByCourse(
            @PathVariable UUID courseId,
            Authentication authentication
    ) {
        User student = (User) authentication.getPrincipal();
        return progressService.getProgressForStudentAndCourse(student.getId(), courseId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/{courseId}/summary")
    public ResponseEntity<CourseProgressSummaryDTO> getSummary(
            @PathVariable UUID courseId,
            Authentication authentication
    ) {
        User student = (User) authentication.getPrincipal();
        return progressService.getSummary(student.getId(), courseId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(
                        CourseProgressSummaryDTO.builder()
                                .courseId(courseId)
                                .completedLessons(0)
                                .totalLessons(0)
                                .percent(0)
                                .status(LearningProgress.ProgressStatus.IN_PROGRESS)
                                .build()
                ));
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/{courseId}/lessons-progress")
    public ResponseEntity<List<LessonProgressDTO>> getLessonsProgress(
            @PathVariable UUID courseId,
            Authentication authentication
    ) {
        User student = (User) authentication.getPrincipal();
        return ResponseEntity.ok(progressService.getUserCourseProgress(student.getId(), courseId));
    }
}
