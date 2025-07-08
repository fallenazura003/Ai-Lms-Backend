package com.forsakenecho.learning_management_system.controller;

import com.forsakenecho.learning_management_system.dto.CourseSummaryDTO;
import com.forsakenecho.learning_management_system.service.CourseService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import java.util.List;

@RestController
@RequestMapping("/api/public/courses")
@RequiredArgsConstructor
public class PublicCourseController {

    private final CourseService courseService;

    @GetMapping("/search")
    // ✅ Cập nhật tham số để nhận Pageable và trả về Page<CourseSummaryDTO>
    public ResponseEntity<Page<CourseSummaryDTO>> searchCourses(
            @RequestParam("q") String keyword,
            Pageable pageable // Spring sẽ tự động inject Pageable từ các tham số page, size, sort
    ) {
        Page<CourseSummaryDTO> results = courseService.searchCourses(keyword, pageable);
        return ResponseEntity.ok(results);
    }
}
