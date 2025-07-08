// src/main/java/com/forsakenecho/learning_management_system/controller/StudentLessonController.java
package com.forsakenecho.learning_management_system.controller;

import com.forsakenecho.learning_management_system.dto.LessonResponse;
import com.forsakenecho.learning_management_system.service.LessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/student/courses/{courseId}/lessons")
@RequiredArgsConstructor
public class StudentLessonController {

    private final LessonService lessonService;

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')") // Chỉ cho phép người dùng có ROLE_STUDENT truy cập
    public ResponseEntity<List<LessonResponse>> getLessonsByCourseIdForStudent(@PathVariable UUID courseId) {
        // Sử dụng lại service đã có để lấy danh sách bài học
        List<LessonResponse> lessons = lessonService.getLessonsByCourseId(courseId);
        return new ResponseEntity<>(lessons, HttpStatus.OK);
    }

    // Các endpoint khác liên quan đến bài học cho học sinh (nếu cần, ví dụ: đánh dấu hoàn thành bài học)
    // sẽ được thêm vào đây sau.
}