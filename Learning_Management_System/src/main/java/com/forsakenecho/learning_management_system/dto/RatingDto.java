package com.forsakenecho.learning_management_system.dto;

import com.forsakenecho.learning_management_system.entity.Rating;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class RatingDto {
    private UUID id;
    private UUID courseId;
    private UUID studentId;
    private int value;
    private LocalDateTime ratedAt;

    public static RatingDto from(Rating save) {
        return RatingDto.builder()
                .id(save.getId())
                .courseId(save.getCourse().getId())
                .studentId(save.getStudent().getId())
                .value(save.getValue())
                .ratedAt(save.getRatedAt())
                .build();
    }
}
