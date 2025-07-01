// src/main/java/com/forsakenecho/learning_management_system/dto/TeacherDashboardStats.java
package com.forsakenecho.learning_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherDashboardStats {
    private long totalCoursesSold;       // Tổng số khóa học đã bán (distinct courses)
    private long totalPurchases;         // Tổng số lượt mua (tổng số record trong CourseManagement với accessType = PURCHASED)
    private double totalRevenue;         // Tổng số tiền thu được
}