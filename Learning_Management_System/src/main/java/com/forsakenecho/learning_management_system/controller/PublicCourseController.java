package com.forsakenecho.learning_management_system.controller;

import com.forsakenecho.learning_management_system.dto.CourseResponse;
import com.forsakenecho.learning_management_system.entity.User;
import com.forsakenecho.learning_management_system.repository.UserRepository;
import com.forsakenecho.learning_management_system.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/public/courses")
@RequiredArgsConstructor
public class PublicCourseController {

    private final CourseService courseService;
    private final UserRepository userRepository;

    // ✅ Cập nhật phương thức searchCourses để nhận tham số 'category'
    @GetMapping("/search")
    public ResponseEntity<Page<CourseResponse>> searchCourses(
            @RequestParam("q") String keyword,
            @RequestParam(value = "category", required = false) String category, // ✅ Thêm tham số category, không bắt buộc
            Pageable pageable
    ) {
        // Truyền tham số category mới vào service
        Page<CourseResponse> results = courseService.searchCourses(keyword, category, pageable);
        return ResponseEntity.ok(results);
    }

    // ✅ Endpoint để lấy tất cả các danh mục có sẵn
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        List<String> categories = courseService.getAllDistinctCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/purchased-course-ids")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UUID>> getPurchasedCourseIds(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User currentUser = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Người dùng không tồn tại."));

        List<UUID> purchasedIds = courseService.getPurchasedCourseIds(currentUser.getId());
        return ResponseEntity.ok(purchasedIds);
    }



}