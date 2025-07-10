package com.forsakenecho.learning_management_system.controller;

import com.forsakenecho.learning_management_system.dto.CourseResponse;
import com.forsakenecho.learning_management_system.entity.User; // Import User entity
import com.forsakenecho.learning_management_system.repository.UserRepository; // Import UserRepository
import com.forsakenecho.learning_management_system.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus; // Import HttpStatus
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Thêm import này
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException; // Import ResponseStatusException

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/public/courses")
@RequiredArgsConstructor
public class PublicCourseController {

    private final CourseService courseService;
    private final UserRepository userRepository; // ✅ Inject UserRepository

    @GetMapping("/search")
    public ResponseEntity<Page<CourseResponse>> searchCourses(
            @RequestParam("q") String keyword,
            Pageable pageable
    ) {
        Page<CourseResponse> results = courseService.searchCourses(keyword, pageable);
        return ResponseEntity.ok(results);
    }

    // ✅ THÊM ENDPOINT MỚI: Để frontend có thể lấy danh sách ID khóa học đã mua
    // ✅ Endpoint này cần được bảo vệ, chỉ cho phép người dùng đã xác thực.
    @GetMapping("/purchased-course-ids")
    @PreAuthorize("isAuthenticated()") // ✅ Chỉ cho phép người dùng đã xác thực truy cập
    public ResponseEntity<List<UUID>> getPurchasedCourseIds(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        // userDetails sẽ không null nếu @PreAuthorize("isAuthenticated()") được áp dụng
        User currentUser = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Người dùng không tồn tại."));

        List<UUID> purchasedIds = courseService.getPurchasedCourseIds(currentUser.getId());
        return ResponseEntity.ok(purchasedIds);
    }
}