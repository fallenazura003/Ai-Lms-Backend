package com.forsakenecho.learning_management_system.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CommentRequest {
    private String content;
    private UUID parentId;
}
