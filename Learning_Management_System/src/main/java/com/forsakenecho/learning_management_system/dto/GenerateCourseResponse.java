// dto/GenerateCourseResponse.java
package com.forsakenecho.learning_management_system.dto;

import com.forsakenecho.learning_management_system.enums.CourseCategory;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GenerateCourseResponse {
    private String title;
    private String description;
    private CourseCategory category;
    private double price;
}
