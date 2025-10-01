package com.forsakenecho.learning_management_system.repository;

import com.forsakenecho.learning_management_system.dto.RevenueChartDTO; // Cần import DTO này
import com.forsakenecho.learning_management_system.entity.CourseManagement;
import com.forsakenecho.learning_management_system.enums.CourseAccessType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseManagementRepository extends JpaRepository<CourseManagement, UUID> {
    List<CourseManagement> findByUserIdAndAccessType(UUID userId, CourseAccessType accessType);
    Optional<CourseManagement> findByUserIdAndCourseIdAndAccessType(UUID userId, UUID courseId, CourseAccessType accessType);

    Page<CourseManagement> findByUserIdAndAccessType(UUID userId, CourseAccessType accessType, Pageable pageable);

    // ✅ PHƯƠNG THỨC MỚI: Chỉ lấy các CourseManagement mà khóa học liên quan đang hiển thị
    @Query("SELECT cm FROM CourseManagement cm WHERE cm.user.id = :userId AND cm.accessType = :accessType AND cm.course.visible = true")
    Page<CourseManagement> findByUserIdAndAccessTypeAndCourseVisibleTrue(UUID userId, CourseAccessType accessType, Pageable pageable);

    // ✅ MỚI: Đếm số lượt mua (tổng số record) cho các khóa học do một giáo viên tạo
    @Query("SELECT COUNT(cm) FROM CourseManagement cm WHERE cm.course.creator.id = :teacherId AND cm.accessType = 'PURCHASED'")
    long countPurchasesForTeacherCourses(@Param("teacherId") UUID teacherId);

    // ✅ MỚI: Đếm số khóa học DISTINCT đã bán bởi một giáo viên
    @Query("SELECT COUNT(DISTINCT cm.course.id) FROM CourseManagement cm WHERE cm.course.creator.id = :teacherId AND cm.accessType = 'PURCHASED'")
    long countDistinctCoursesSoldByTeacher(@Param("teacherId") UUID teacherId);

    // ✅ MỚI: Tính tổng doanh thu cho các khóa học của một giáo viên
    @Query("SELECT SUM(cm.course.price) FROM CourseManagement cm WHERE cm.course.creator.id = :teacherId AND cm.accessType = 'PURCHASED'")
    Double sumRevenueForTeacherCourses(@Param("teacherId") UUID teacherId);

    // ✅ MỚI: Lấy chi tiết các giao dịch mua cho các khóa học của một giáo viên
    @Query("SELECT cm FROM CourseManagement cm WHERE cm.course.creator.id = :teacherId AND cm.accessType = 'PURCHASED' ORDER BY cm.purchasedAt DESC")
    Page<CourseManagement> findPurchasesByTeacherId(@Param("teacherId") UUID teacherId, Pageable pageable);

    // ✅ MỚI: Lấy danh sách người mua cho một khóa học cụ thể do giáo viên sở hữu
    @Query("SELECT cm FROM CourseManagement cm WHERE cm.course.id = :courseId AND cm.accessType = 'PURCHASED'")
    List<CourseManagement> findBuyersByCourseId(@Param("courseId") UUID courseId);

    List<CourseManagement> findByCourseIdAndAccessType(UUID courseId, CourseAccessType courseAccessType);

    // ⭐ PHƯƠNG THỨC MỚI CHO BIỂU ĐỒ DOANH THU
    /**
     * Truy vấn dữ liệu doanh thu được nhóm theo tháng và năm của giao dịch.
     * Sử dụng Constructor Expression để ánh xạ trực tiếp sang RevenueChartDTO.
     * Sắp xếp từ cũ nhất đến mới nhất.
     */
    @Query("""
        SELECT new com.forsakenecho.learning_management_system.dto.RevenueChartDTO(
            YEAR(cm.purchasedAt),
            MONTH(cm.purchasedAt),
            SUM(cm.course.price)
        )
        FROM CourseManagement cm 
        WHERE cm.course.creator.id = :teacherId AND cm.accessType = 'PURCHASED'
        GROUP BY YEAR(cm.purchasedAt), MONTH(cm.purchasedAt)
        ORDER BY YEAR(cm.purchasedAt) ASC, MONTH(cm.purchasedAt) ASC
    """)
    List<RevenueChartDTO> findMonthlyRevenueByTeacherId(@Param("teacherId") UUID teacherId);
}
