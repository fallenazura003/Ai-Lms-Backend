package com.forsakenecho.learning_management_system.dto;

import com.forsakenecho.learning_management_system.enums.CourseCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreateCourseRequest {
    private String title;
    private String description;
    private CourseCategory category;
    private Double price;
    private String image;
}
