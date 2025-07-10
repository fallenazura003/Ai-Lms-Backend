package com.forsakenecho.learning_management_system.dto;

import com.forsakenecho.learning_management_system.entity.Course;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CourseSummaryDTO {
    private UUID id;
    private String title;
    private String description;
    private String imageUrl;
    private Double price;
    private String createdBy;
    private LocalDateTime createdAt;
    private boolean visible;

    public static CourseSummaryDTO fromEntity(Course course) {
        return CourseSummaryDTO.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .price(course.getPrice())
                .imageUrl(course.getImageUrl())
                .createdBy(course.getCreator().getName())
                .createdAt(course.getCreatedAt())
                .visible(course.isVisible())
                .build();
    }
}
