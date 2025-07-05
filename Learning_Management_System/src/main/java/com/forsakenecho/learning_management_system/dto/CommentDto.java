package com.forsakenecho.learning_management_system.dto;

import com.forsakenecho.learning_management_system.entity.Comment;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CommentDto {
    private String id;
    private String content;
    private String userName;
    private String userId;
    private String parentId;
    private LocalDateTime createdAt;
    private List<CommentDto> replies;

    public static CommentDto from(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId().toString())
                .content(comment.getContent())
                .userName(comment.getAuthor().getName())
                .userId(comment.getAuthor().getId().toString())
                .parentId(comment.getParent() != null ? comment.getParent().getId().toString() : null)
                .createdAt(comment.getCreatedAt())
                .replies(new ArrayList<>())
                .build();
    }
}
