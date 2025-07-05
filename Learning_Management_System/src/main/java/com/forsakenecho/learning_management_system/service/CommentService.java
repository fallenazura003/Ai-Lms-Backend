package com.forsakenecho.learning_management_system.service;

import com.forsakenecho.learning_management_system.dto.CommentDto;
import com.forsakenecho.learning_management_system.entity.Comment;
import com.forsakenecho.learning_management_system.entity.Course;
import com.forsakenecho.learning_management_system.entity.User;
import com.forsakenecho.learning_management_system.enums.CourseAccessType;
import com.forsakenecho.learning_management_system.repository.CommentRepository;
import com.forsakenecho.learning_management_system.repository.CourseManagementRepository;
import com.forsakenecho.learning_management_system.repository.CourseRepository;
import com.forsakenecho.learning_management_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CourseRepository courseRepository;
    private final CourseManagementRepository courseManagementRepository;
    private final UserRepository userRepository;

    public CommentDto addComment(UUID courseId, String content, UUID parentId, User user) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Khóa học không tồn tại"));

        boolean isTeacherOfCourse = course.getCreator().getId().equals(user.getId());
        boolean isStudentAndPurchased = courseManagementRepository
                .findByUserIdAndCourseIdAndAccessType(user.getId(), courseId, CourseAccessType.PURCHASED)
                .isPresent();

        if (!(isTeacherOfCourse || isStudentAndPurchased)) {
            throw new RuntimeException("Bạn không có quyền bình luận khóa học này");
        }

        Comment parent = null;
        if (parentId != null) {
            parent = commentRepository.findById(parentId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bình luận gốc"));
        }

        Comment comment = Comment.builder()
                .course(course)
                .content(content)
                .parent(parent)
                .author(user)
                .build();

        return CommentDto.from(commentRepository.save(comment));
    }

    public List<CommentDto> getCommentsByCourse(UUID courseId) {
        List<Comment> comments = commentRepository.findByCourseIdOrderByCreatedAtAsc(courseId);

        // B1: Chuyển sang DTO
        Map<String, CommentDto> dtoMap = comments.stream()
                .map(CommentDto::from)
                .collect(Collectors.toMap(CommentDto::getId, Function.identity()));

        // B2: Xây cây reply
        List<CommentDto> roots = new ArrayList<>();
        for (Comment comment : comments) {
            CommentDto dto = dtoMap.get(comment.getId().toString()); // ✅ Sửa ở đây
            if (dto == null) continue;

            if (comment.getParent() != null) {
                String parentId = comment.getParent().getId().toString(); // ✅ Sửa ở đây
                CommentDto parentDto = dtoMap.get(parentId);
                if (parentDto != null) {
                    parentDto.getReplies().add(dto);
                }
            } else {
                roots.add(dto);
            }
        }
        return roots;
    }

    public CommentDto updateComment(UUID commentId, String newContent, User user) {
        Comment comment = commentRepository.findByIdAndAuthor_Id(commentId, user.getId())
                .orElseThrow(() -> new RuntimeException("Bạn không có quyền chỉnh sửa bình luận này"));

        comment.setContent(newContent);
        return CommentDto.from(commentRepository.save(comment));
    }

    public void deleteComment(UUID commentId, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bình luận"));

        if (!comment.getAuthor().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền xóa bình luận này");
        }

        commentRepository.delete(comment);
    }
}

