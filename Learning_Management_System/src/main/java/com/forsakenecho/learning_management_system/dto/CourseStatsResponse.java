package com.forsakenecho.learning_management_system.dto;

import com.forsakenecho.learning_management_system.entity.CourseManagement;
import com.forsakenecho.learning_management_system.entity.LearningProgress; // ✅ Import mới
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
public class CourseStatsResponse {
    private String courseTitle;
    private Long totalRevenue;
    private Long totalEnrollments;
    private List<StudentProgress> students;

    @Getter
    @Setter
    public static class StudentProgress {
        private UUID studentId;
        private String studentName;
        private String studentEmail;
        private LocalDateTime purchaseDate; // ✅ Đổi tên từ enrollmentDate thành purchaseDate
        private double progressPercentage;
        private int completedLessons;
        private int totalLessons;


        public StudentProgress(
                UUID studentId,
                String studentName,
                String studentEmail,
                LocalDateTime purchaseDate,
                double progressPercentage,
                int completedLessons,
                int totalLessons) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.studentEmail = studentEmail;
            this.purchaseDate = purchaseDate;
            this.progressPercentage = progressPercentage;
            this.completedLessons = completedLessons;
            this.totalLessons = totalLessons;
        }

        /**
         * Ánh xạ dữ liệu học viên (từ CourseManagement) và tiến độ (từ LearningProgress).
         * @param cm Dữ liệu đăng ký khóa học (dùng để lấy thông tin User và PurchaseDate).
         * @param lp Dữ liệu tiến độ học tập (dùng để lấy số bài đã hoàn thành).
         * @param totalLessons Tổng số bài học trong khóa học.
         * @return StudentProgress DTO.
         */
        public static StudentProgress from(CourseManagement cm, LearningProgress lp, int totalLessons) {

            // Lấy số lượng bài học đã hoàn thành từ LearningProgress (DỮ LIỆU ĐƯỢC CHUYỂN HOÀN TOÀN TỪ LEARNING PROGRESS)
            long completedLessonsCount = lp.getCompletedLessonIds() != null ? lp.getCompletedLessonIds().size() : 0;

            // Tính phần trăm tiến độ
            double progress = totalLessons > 0 ? (double) completedLessonsCount / totalLessons * 100 : 0.0;

            return new StudentProgress(
                    cm.getUser().getId(),
                    cm.getUser().getName(),
                    cm.getUser().getEmail(),
                    cm.getPurchasedAt(), // ✅ SỬA LỖI: Sử dụng getPurchasedAt() thay vì getEnrollmentDate()
                    Math.round(progress * 10.0) / 10.0, // Làm tròn 1 chữ số thập phân
                    (int) completedLessonsCount,
                    totalLessons
            );
        }
    }

    // ✅ PHƯƠNG THỨC FROM MỚI: Nhận vào danh sách đã được kết hợp
    public static CourseStatsResponse from(
            String title,
            Long revenue,
            List<StudentProgress> studentProgressList) {

        return CourseStatsResponse.builder()
                .courseTitle(title)
                .totalRevenue(revenue)
                .totalEnrollments((long) studentProgressList.size())
                .students(studentProgressList)
                .build();
    }
}
