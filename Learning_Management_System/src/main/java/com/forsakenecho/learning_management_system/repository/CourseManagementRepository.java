package com.forsakenecho.learning_management_system.repository;

import com.forsakenecho.learning_management_system.dto.RevenueChartDTO;
import com.forsakenecho.learning_management_system.entity.CourseManagement;
import com.forsakenecho.learning_management_system.enums.CourseAccessType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseManagementRepository extends JpaRepository<CourseManagement, UUID> {

    // --- Các phương thức hiện có ---
    List<CourseManagement> findByUserIdAndAccessType(UUID userId, CourseAccessType accessType);
    Optional<CourseManagement> findByUserIdAndCourseIdAndAccessType(UUID userId, UUID courseId, CourseAccessType accessType);
    Page<CourseManagement> findByUserIdAndAccessType(UUID userId, CourseAccessType accessType, Pageable pageable);

    @Query("SELECT cm FROM CourseManagement cm WHERE cm.user.id = :userId AND cm.accessType = :accessType AND cm.course.visible = true")
    Page<CourseManagement> findByUserIdAndAccessTypeAndCourseVisibleTrue(UUID userId, CourseAccessType accessType, Pageable pageable);

    // --- Thống kê cho Giáo viên ---
    @Query("SELECT COUNT(cm) FROM CourseManagement cm WHERE cm.course.creator.id = :teacherId AND cm.accessType = 'PURCHASED'")
    long countPurchasesForTeacherCourses(@Param("teacherId") UUID teacherId);

    @Query("SELECT COUNT(DISTINCT cm.course.id) FROM CourseManagement cm WHERE cm.course.creator.id = :teacherId AND cm.accessType = 'PURCHASED'")
    long countDistinctCoursesSoldByTeacher(@Param("teacherId") UUID teacherId);

    @Query("SELECT COALESCE(SUM(cm.course.price), 0.0) FROM CourseManagement cm WHERE cm.course.creator.id = :teacherId AND cm.accessType = 'PURCHASED'")
    Double sumRevenueForTeacherCourses(@Param("teacherId") UUID teacherId);

    @Query("SELECT cm FROM CourseManagement cm WHERE cm.course.creator.id = :teacherId AND cm.accessType = 'PURCHASED' ORDER BY cm.purchasedAt DESC")
    Page<CourseManagement> findPurchasesByTeacherId(@Param("teacherId") UUID teacherId, Pageable pageable);

    @Query("SELECT cm FROM CourseManagement cm WHERE cm.course.id = :courseId AND cm.accessType = 'PURCHASED'")
    List<CourseManagement> findBuyersByCourseId(@Param("courseId") UUID courseId);

    // ⭐ PHƯƠNG THỨC MỚI CHO BIỂU ĐỒ DOANH THU
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

    // --- Phương thức cho Thống kê Khóa học (Course Stats) ---

    // Kiểm tra xem người dùng đã đăng ký khóa học chưa
    boolean existsByCourseIdAndUserId(UUID courseId, UUID userId);

    // Đếm số lượt mua (số học viên)
    long countByCourseId(UUID courseId);

    // Tính tổng doanh thu cho khóa học (ĐÃ SỬA: dùng cm.course.price)
    @Query("SELECT COALESCE(SUM(cm.course.price), 0) FROM CourseManagement cm WHERE cm.course.id = :courseId AND cm.accessType = 'PURCHASED'")
    Long calculateTotalRevenue(@Param("courseId") UUID courseId);

    // Lấy danh sách các học viên đã mua khóa học cùng với tiến độ của họ (SỬA: không cần JOIN FETCH vì User đã là một phần của CourseManagement)
    @Query("SELECT cm FROM CourseManagement cm WHERE cm.course.id = :courseId AND cm.accessType = 'PURCHASED'")
    List<CourseManagement> findStudentsAndProgressByCourseId(@Param("courseId") UUID courseId);

    List<CourseManagement> findByCourseIdAndAccessType(UUID courseId, CourseAccessType courseAccessType);

    Long countByCourseIdAndAccessType(UUID courseId, CourseAccessType courseAccessType);

}
