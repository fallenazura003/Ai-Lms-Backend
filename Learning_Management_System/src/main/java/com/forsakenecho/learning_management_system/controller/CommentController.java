package com.forsakenecho.learning_management_system.controller;

import com.forsakenecho.learning_management_system.dto.CommentDto;
import com.forsakenecho.learning_management_system.dto.CommentRequest;
import com.forsakenecho.learning_management_system.entity.User;
import com.forsakenecho.learning_management_system.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/student/courses/{courseId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // ✅ Thêm bình luận (học sinh hoặc giáo viên — đã kiểm tra logic ở service)
    @PostMapping
    public ResponseEntity<CommentDto> addComment(
            @PathVariable UUID courseId,
            @RequestBody CommentRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        CommentDto created = commentService.addComment(courseId, request.getContent(), request.getParentId(), user);
        return ResponseEntity.ok(created);
    }

    // ✅ Lấy danh sách bình luận
    @GetMapping
    public ResponseEntity<List<CommentDto>> getComments(@PathVariable UUID courseId) {
        List<CommentDto> comments = commentService.getCommentsByCourse(courseId);
        return ResponseEntity.ok(comments);
    }

    // ✅ Cập nhật bình luận
    @PutMapping("/{commentId}")
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

    // xóa bình luận
    @DeleteMapping("/{commentId}")
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
