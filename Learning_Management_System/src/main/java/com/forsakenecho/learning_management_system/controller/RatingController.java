package com.forsakenecho.learning_management_system.controller;

import com.forsakenecho.learning_management_system.dto.RatingDto;
import com.forsakenecho.learning_management_system.dto.RatingRequest;
import com.forsakenecho.learning_management_system.service.RatingService;
import com.forsakenecho.learning_management_system.entity.User;
import jakarta.validation.Valid; // Đảm bảo import này được sử dụng nếu bạn dùng @Valid
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize; // ✅ Import PreAuthorize

import java.util.UUID;

@RestController
@RequestMapping("/api/student/courses/{courseId}/rating")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    /**
     * Endpoint để thêm hoặc cập nhật đánh giá của học sinh cho khóa học.
     * Chỉ học sinh đã đăng nhập mới có thể thực hiện đánh giá.
     */
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')") // ✅ Chỉ học sinh mới có quyền đánh giá
    public ResponseEntity<?> rate(@PathVariable UUID courseId,
                                  @RequestBody RatingRequest request,
                                  Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        ratingService.addOrUpdateRating(courseId, request.getValue(), user);
        return ResponseEntity.ok("Đánh giá thành công");
    }

    /**
     * Endpoint để lấy điểm đánh giá trung bình của một khóa học.
     * Thông tin này thường hiển thị công khai trên trang chi tiết khóa học,
     * nên có thể cho phép tất cả mọi người (kể cả chưa đăng nhập) hoặc mọi vai trò xem.
     * Chọn 'permitAll()' để đơn giản nhất cho việc hiển thị trên CourseCard hoặc trang chi tiết.
     */
    @GetMapping("/average")
    @PreAuthorize("permitAll()") // ✅ Cho phép mọi người xem điểm đánh giá trung bình
    public Double getAverage(@PathVariable UUID courseId) {
        return ratingService.getAverageRating(courseId);
    }

    /**
     * Endpoint để lấy đánh giá của người dùng hiện tại về khóa học.
     * Chỉ học sinh mới có "đánh giá của tôi" cho một khóa học.
     * Giáo viên hoặc các vai trò khác không có khái niệm "đánh giá của tôi" từ góc độ học viên.
     */
    @GetMapping("/mine")
    @PreAuthorize("hasRole('STUDENT')") // ✅ Chỉ học sinh mới có quyền xem đánh giá của riêng mình
    public RatingDto getMyRating(@PathVariable UUID courseId,
                                 Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ratingService.getUserRating(courseId, user.getId());
    }
}