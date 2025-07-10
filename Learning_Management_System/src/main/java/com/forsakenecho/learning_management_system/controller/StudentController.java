// StudentController.java
package com.forsakenecho.learning_management_system.controller;

import com.forsakenecho.learning_management_system.dto.*;
import com.forsakenecho.learning_management_system.entity.Course;
import com.forsakenecho.learning_management_system.entity.CourseManagement;
import com.forsakenecho.learning_management_system.entity.User;
import com.forsakenecho.learning_management_system.enums.CourseAccessType;
import com.forsakenecho.learning_management_system.repository.CourseManagementRepository;
import com.forsakenecho.learning_management_system.repository.CourseRepository;
import com.forsakenecho.learning_management_system.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final CourseService courseService;
    private final CourseRepository courseRepository;
    private final CourseManagementRepository courseManagementRepository;

    // Helper method to get current user
    private User getCurrentUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }

    // Lấy danh sách khóa học đã mua (KHÔNG CẦN LỌC THEO VISIBLE Ở ĐÂY, vì đã mua thì luôn thấy)
    @GetMapping("/courses")
    public ResponseEntity<Page<CourseResponse>> getPurchasedCourses(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        User user = getCurrentUser(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<Course> courses = courseService.getCoursesByUserAndAccessType(user.getId(), CourseAccessType.PURCHASED, pageable);
        Page<CourseResponse> response = courses.map(CourseResponse::from);
        return ResponseEntity.ok(response);
    }

    // Mua khóa học
    @PostMapping("/purchase")
    public ResponseEntity<ApiResponse<PurchaseResponse>> purchaseCourse(@RequestBody PurchaseCourseRequest request, Authentication authentication) {
        User student = getCurrentUser(authentication);

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        // THÊM KIỂM TRA visible ở đây TRƯỚC KHI CHO PHÉP MUA
        if (!course.isVisible()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.<PurchaseResponse>builder()
                    .message("Khóa học này hiện không khả dụng để mua.")
                    .timestamp(LocalDateTime.now())
                    .build());
        }

        boolean alreadyPurchased = courseManagementRepository
                .findByUserIdAndCourseIdAndAccessType(student.getId(), course.getId(), CourseAccessType.PURCHASED)
                .isPresent();

        if (alreadyPurchased) {
            return ResponseEntity.badRequest().body(ApiResponse.<PurchaseResponse>builder()
                    .message("Khóa học đã được mua rồi.")
                    .timestamp(LocalDateTime.now())
                    .build());
        }

        CourseManagement courseManagement = CourseManagement.builder()
                .user(student)
                .course(course)
                .accessType(CourseAccessType.PURCHASED)
                .build();

        courseManagementRepository.save(courseManagement);

        PurchaseResponse purchaseResponse = PurchaseResponse.builder()
                .studentId(student.getId())
                .courseId(course.getId())
                .purchasedAt(courseManagement.getPurchasedAt())
                .build();

        ApiResponse<PurchaseResponse> response = ApiResponse.<PurchaseResponse>builder()
                .message("Mua khóa học thành công!")
                .data(purchaseResponse)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    // Lấy chi tiết khóa học (kèm kiểm tra khóa học có bị ẩn không)
    @GetMapping("/courses/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<CourseResponse> getCourseDetail(@PathVariable UUID id, Authentication authentication) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        User student = getCurrentUser(authentication);

        // Kiểm tra quyền truy cập:
        // 1. Nếu khóa học bị ẩn
        // 2. VÀ học sinh CHƯA mua khóa học đó
        boolean isPurchased = courseManagementRepository
                .findByUserIdAndCourseIdAndAccessType(student.getId(), course.getId(), CourseAccessType.PURCHASED)
                .isPresent();

        if (!course.isVisible() && !isPurchased) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Khóa học này hiện không khả dụng.");
        }
        // Nếu khóa học bị ẩn nhưng đã mua, vẫn cho phép truy cập.
        // Nếu khóa học hiển thị, cho phép truy cập.

        return ResponseEntity.ok(CourseResponse.from(course));
    }


    // Lấy danh sách khóa học đang visible nhưng học sinh CHƯA mua (explore)
    @GetMapping("/explore")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Page<CourseResponse>> getExploreCourses(
            Authentication authentication,
            @RequestParam(value = "category", required = false) String category, // ✅ Thêm category
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size
    ) {
        User student = getCurrentUser(authentication);
        Pageable pageable = PageRequest.of(page, size);

        // ✅ Gọi phương thức mới trong CourseService để lọc explore courses
        Page<CourseResponse> exploreCourses = courseService.getExploreCoursesForStudent(student.getId(), category, pageable);

        return ResponseEntity.ok(exploreCourses);
    }

    // Kiểm tra học sinh đã mua một khóa học cụ thể chưa
    @GetMapping("/enrolled/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Boolean> isStudentEnrolled(@PathVariable UUID courseId, Authentication authentication) {
        User student = getCurrentUser(authentication);

        boolean isEnrolled = courseManagementRepository
                .findByUserIdAndCourseIdAndAccessType(student.getId(), courseId, CourseAccessType.PURCHASED)
                .isPresent();

        return ResponseEntity.ok(isEnrolled); // CHỈ trả true nếu đã mua
    }

    // Endpoint để lấy danh sách ID khóa học đã mua (được gọi từ PublicCourseController)
    // Endpoint này đã được di chuyển sang PublicCourseController
    // @GetMapping("/purchased-course-ids")
    // @PreAuthorize("isAuthenticated()")
    // public ResponseEntity<List<UUID>> getPurchasedCourseIds(
    //         @AuthenticationPrincipal UserDetails userDetails
    // ) {
    //     User currentUser = userRepository.findByEmail(userDetails.getUsername())
    //             .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Người dùng không tồn tại."));
    //
    //     List<UUID> purchasedIds = courseService.getPurchasedCourseIds(currentUser.getId());
    //     return ResponseEntity.ok(purchasedIds);
    // }
}