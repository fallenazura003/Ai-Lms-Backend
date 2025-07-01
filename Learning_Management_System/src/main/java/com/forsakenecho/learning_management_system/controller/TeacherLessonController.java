package com.forsakenecho.learning_management_system.controller;

import com.forsakenecho.learning_management_system.dto.GenerateLessonResponse;
import com.forsakenecho.learning_management_system.dto.LessonRequest;
import com.forsakenecho.learning_management_system.dto.LessonResponse; // ✅ Import LessonResponse
import com.forsakenecho.learning_management_system.entity.Lesson; // Giữ lại nếu cần cho việc map nội bộ hoặc logging
import com.forsakenecho.learning_management_system.service.AiLessonGeneratorService;
import com.forsakenecho.learning_management_system.service.LessonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/teacher/courses/{courseId}/lessons")
@RequiredArgsConstructor
public class TeacherLessonController {
    private final LessonService lessonService;
    private final AiLessonGeneratorService  aiLessonGeneratorService;

    // tạo mới lesson
    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<LessonResponse> createLesson( // ✅ Kiểu trả về LessonResponse
                                                        @PathVariable UUID courseId,
                                                        @Valid @RequestBody LessonRequest lessonRequest) {
        LessonResponse createdLesson = lessonService.createLesson(courseId, lessonRequest); // ✅ Gọi service trả về LessonResponse
        return new ResponseEntity<>(createdLesson, HttpStatus.CREATED);
    }

    // updateLesson
    @PutMapping("/{lessonId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<LessonResponse> updateLesson( // ✅ Kiểu trả về LessonResponse
                                                        @PathVariable UUID courseId,
                                                        @PathVariable UUID lessonId,
                                                        @Valid @RequestBody LessonRequest lessonRequest) {
        LessonResponse updatedLesson = lessonService.updateLesson(courseId, lessonId, lessonRequest); // ✅ Gọi service trả về LessonResponse
        return new ResponseEntity<>(updatedLesson, HttpStatus.OK);
    }

    // getLessonsByCourseId
    @GetMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<LessonResponse>> getLessonsByCourseId(@PathVariable UUID courseId) { // ✅ Kiểu trả về List<LessonResponse>
        List<LessonResponse> lessons = lessonService.getLessonsByCourseId(courseId); // ✅ Gọi service trả về List<LessonResponse>
        return new ResponseEntity<>(lessons, HttpStatus.OK);
    }

    // delete Lesson
    @DeleteMapping("/{lessonId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> deleteLesson(
            @PathVariable UUID courseId,
            @PathVariable UUID lessonId) {
        lessonService.deleteLesson(courseId, lessonId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // Phương thức để lấy một bài học cụ thể theo ID (có thể cần cho frontend xem chi tiết)
    @GetMapping("/{lessonId}")
    @PreAuthorize("hasRole('TEACHER')") // Hoặc @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')") nếu học sinh cũng xem được
    public ResponseEntity<LessonResponse> getLessonById(@PathVariable UUID lessonId) {
        LessonResponse lesson = lessonService.getLessonById(lessonId);
        return new ResponseEntity<>(lesson, HttpStatus.OK);
    }


}