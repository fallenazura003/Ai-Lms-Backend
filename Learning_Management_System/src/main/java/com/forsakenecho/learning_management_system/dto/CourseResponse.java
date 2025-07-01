package com.forsakenecho.learning_management_system.dto;

import com.forsakenecho.learning_management_system.entity.Course;
import com.forsakenecho.learning_management_system.enums.CourseCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CourseResponse {
    private UUID id;
    private String title;
    private String description;
    private Double price;
    private CourseCategory category;
    private String creatorName;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean visible;


    public static CourseResponse from(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .price(course.getPrice())
                .category(course.getCategory())
                .creatorName(course.getCreator().getName()) // hoặc getUsername()
                .imageUrl(course.getImageUrl())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .visible(course.isVisible())
                .build();
    }
}

