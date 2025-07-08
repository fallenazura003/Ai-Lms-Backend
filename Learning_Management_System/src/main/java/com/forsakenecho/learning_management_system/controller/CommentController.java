package com.forsakenecho.learning_management_system.controller;

import com.forsakenecho.learning_management_system.dto.CommentDto;
import com.forsakenecho.learning_management_system.dto.CommentRequest;
import com.forsakenecho.learning_management_system.entity.User;
import com.forsakenecho.learning_management_system.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // ✅ Import PreAuthorize
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/student/courses/{courseId}/comments") // Giữ nguyên đường dẫn này
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * Thêm bình luận hoặc phản hồi bình luận.
     * Cả học sinh và giáo viên đều có thể thêm bình luận/phản hồi.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')") // ✅ Học sinh hoặc Giáo viên
    public ResponseEntity<CommentDto> addComment(
            @PathVariable UUID courseId,
            @RequestBody CommentRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        CommentDto created = commentService.addComment(courseId, request.getContent(), request.getParentId(), user);
        return ResponseEntity.ok(created);
    }

    /**
     * Lấy danh sách tất cả bình luận của một khóa học.
     * Thông tin bình luận thường có thể được xem bởi mọi người dùng truy cập trang,
     * bao gồm cả người chưa đăng nhập. Nếu chỉ muốn người đã đăng nhập xem, dùng hasAnyRole.
     * Chọn permitAll() để đơn giản cho việc hiển thị public.
     */
    @GetMapping
    @PreAuthorize("permitAll()") // ✅ Cho phép mọi người xem bình luận
    public ResponseEntity<List<CommentDto>> getComments(@PathVariable UUID courseId) {
        List<CommentDto> comments = commentService.getCommentsByCourse(courseId);
        return ResponseEntity.ok(comments);
    }

    /**
     * Cập nhật nội dung bình luận.
     * Chỉ người tạo bình luận hoặc admin mới có quyền sửa.
     * Nếu bạn muốn giáo viên cũng có thể sửa bình luận của họ, hãy thêm TEACHER vào.
     * (Logic kiểm tra người tạo đã có trong service của bạn).
     */
    @PutMapping("/{commentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')") // ✅ Người tạo (STUDENT/TEACHER) hoặc ADMIN
    public ResponseEntity<?> updateComment(
            @PathVariable UUID courseId,
            @PathVariable UUID commentId,
            @RequestBody Map<String, String> body,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        String content = body.get("content");

        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Nội dung không được để trống");
        }

        CommentDto updated = commentService.updateComment(commentId, content.trim(), user);
        return ResponseEntity.ok(updated);
    }

    /**
     * Xóa bình luận.
     * Người tạo bình luận, giáo viên (đối với khóa học của họ), hoặc admin có thể xóa bình luận.
     * (Logic kiểm tra người tạo/quyền của giáo viên đã có trong service của bạn).
     */
    @DeleteMapping("/{commentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')") // ✅ Người tạo (STUDENT/TEACHER), hoặc ADMIN
    public ResponseEntity<?> deleteComment(
            @PathVariable UUID courseId,
            @PathVariable UUID commentId,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        commentService.deleteComment(commentId, user);
        return ResponseEntity.ok("Xóa bình luận thành công");
    }
}