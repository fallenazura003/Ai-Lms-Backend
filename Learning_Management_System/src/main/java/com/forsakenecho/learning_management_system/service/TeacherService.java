// src/main/java/com/forsakenecho/learning_management_system/service/TeacherService.java
package com.forsakenecho.learning_management_system.service;

import com.forsakenecho.learning_management_system.dto.BuyerDto;
import com.forsakenecho.learning_management_system.dto.PurchaseDetailDto;
import com.forsakenecho.learning_management_system.dto.TeacherDashboardStats;
import com.forsakenecho.learning_management_system.entity.CourseManagement;
import com.forsakenecho.learning_management_system.repository.CourseManagementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherService {
    private final CourseManagementRepository courseManagementRepository;

    @Transactional(readOnly = true)
    public TeacherDashboardStats getTeacherDashboardStats(UUID teacherId) {
        long totalCoursesSold = courseManagementRepository.countDistinctCoursesSoldByTeacher(teacherId);
        long totalPurchases = courseManagementRepository.countPurchasesForTeacherCourses(teacherId);
        Double totalRevenue = courseManagementRepository.sumRevenueForTeacherCourses(teacherId);

        return TeacherDashboardStats.builder()
                .totalCoursesSold(totalCoursesSold)
                .totalPurchases(totalPurchases)
                .totalRevenue(totalRevenue != null ? totalRevenue : 0.0)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<PurchaseDetailDto> getDetailedPurchaseStats(UUID teacherId, Pageable pageable) {
        Page<CourseManagement> purchasesPage = courseManagementRepository.findPurchasesByTeacherId(teacherId, pageable);

        List<PurchaseDetailDto> dtos = purchasesPage.getContent().stream()
                .map(cm -> PurchaseDetailDto.builder()
                        .purchaseId(cm.getId())
                        .courseId(cm.getCourse().getId())
                        .courseTitle(cm.getCourse().getTitle())
                        .buyerName(cm.getUser().getName())
                        .buyerEmail(cm.getUser().getEmail())
                        .price(cm.getCourse().getPrice()) // Lấy giá hiện tại
                        .purchaseDate(cm.getPurchasedAt())
                        .build())
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, purchasesPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<BuyerDto> getBuyersForCourse(UUID courseId) {
        List<CourseManagement> buyers = courseManagementRepository.findBuyersByCourseId(courseId);

        return buyers.stream()
                .map(cm -> BuyerDto.builder()
                        .userId(cm.getUser().getId())
                        .userName(cm.getUser().getName())
                        .userEmail(cm.getUser().getEmail())
                        .purchaseDate(cm.getPurchasedAt())
                        .build())
                .collect(Collectors.toList());
    }
}