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

import java.time.LocalDateTime;
import java.util.List; // Import này có thể không cần nếu không dùng List trực tiếp
import java.util.UUID;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final CourseService courseService;
    private final CourseRepository courseRepository;
    private final CourseManagementRepository courseManagementRepository;

    // ✅ Lấy danh sách khóa học đã mua (KHÔNG CẦN LỌC THEO VISIBLE Ở ĐÂY, vì đã mua thì luôn thấy)
    @GetMapping("/courses")
    public ResponseEntity<?> getPurchasedCourses(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        User user = (User) authentication.getPrincipal();
        Pageable pageable = PageRequest.of(page, size);
        Page<Course> courses = courseService.getCoursesByUserAndAccessType(user.getId(), CourseAccessType.PURCHASED, pageable);
        Page<CourseResponse> response = courses.map(CourseResponse::from);
        return ResponseEntity.ok(response);
    }

    // ✅ Mua khóa học
    @PostMapping("/purchase")
    public ResponseEntity<?> purchaseCourse(@RequestBody PurchaseCourseRequest request, Authentication authentication) {
        User student = (User) authentication.getPrincipal();

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        // ✅ THÊM KIỂM TRA visible ở đây TRƯỚC KHI CHO PHÉP MUA
        if (!course.isVisible()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Khóa học này hiện không khả dụng để mua.");
        }

        boolean alreadyPurchased = courseManagementRepository
                .findByUserIdAndCourseIdAndAccessType(student.getId(), course.getId(), CourseAccessType.PURCHASED)
                .isPresent();

        if (alreadyPurchased) {
            return ResponseEntity.badRequest().body("Khóa học đã được mua rồi.");
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

    // ✅ Lấy chi tiết khóa học (kèm kiểm tra khóa học có bị ẩn không)
    @GetMapping("/courses/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getCourseDetail(@PathVariable UUID id, Authentication authentication) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        User student = (User) authentication.getPrincipal();

        // Kiểm tra quyền truy cập:
        // 1. Nếu khóa học bị ẩn
        // 2. VÀ học sinh CHƯA mua khóa học đó
        boolean isPurchased = courseManagementRepository
                .findByUserIdAndCourseIdAndAccessType(student.getId(), course.getId(), CourseAccessType.PURCHASED)
                .isPresent();

        if (!course.isVisible() && !isPurchased) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Khóa học này hiện không khả dụng.");
        }
        // Nếu khóa học bị ẩn nhưng đã mua, vẫn cho phép truy cập.
        // Nếu khóa học hiển thị, cho phép truy cập.

        return ResponseEntity.ok(CourseResponse.from(course));
    }


    // ✅ Lấy danh sách khóa học đang visible nhưng học sinh CHƯA mua (explore)
    @GetMapping("/explore")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getVisibleCoursesNotPurchased(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size
    ) {
        User student = (User) authentication.getPrincipal();
        Pageable pageable = PageRequest.of(page, size);

        // ✅ Đảm bảo phương thức này trong CourseService đã lọc visible=true
        Page<Course> pagedCourses = courseService.getVisibleCoursesNotPurchased(student.getId(), pageable);

        Page<CourseResponse> responsePage = pagedCourses.map(CourseResponse::from);

        return ResponseEntity.ok(responsePage);
    }

    // ✅ Kiểm tra học sinh đã mua một khóa học cụ thể chưa
    @GetMapping("/enrolled/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> isStudentEnrolled(@PathVariable UUID courseId, Authentication authentication) {
        User student = (User) authentication.getPrincipal();

        boolean isEnrolled = courseManagementRepository
                .findByUserIdAndCourseIdAndAccessType(student.getId(), courseId, CourseAccessType.PURCHASED)
                .isPresent();

        return ResponseEntity.ok(isEnrolled); // ✅ CHỈ trả true nếu đã mua
    }
}