package com.forsakenecho.learning_management_system.repository;

import com.forsakenecho.learning_management_system.entity.Course;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // ✅ Import này là rất quan trọng
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

// ✅ Kế thừa JpaSpecificationExecutor để có thể sử dụng Specification trong service
public interface CourseRepository extends JpaRepository<Course, UUID>, JpaSpecificationExecutor<Course> {
    // Các phương thức này vẫn cần nếu bạn sử dụng chúng trực tiếp ở đâu đó
    // hoặc cho các truy vấn đơn giản không cần Specification.
    Page<Course> findByVisibleTrue(Pageable pageable);

    Page<Course> findByVisibleTrueAndIdNotIn(List<UUID> excludedIds, Pageable pageable);

    Page<Course> findByCreatorId(UUID userId, Pageable pageable);


    // ✅ PHƯƠNG THỨC MỚI: Để lấy danh sách các danh mục duy nhất cho bộ lọc.
    // Điều này vẫn cần thiết vì Specification không có sẵn chức năng DISTINCT cho một trường cụ thể như thế này.
    @Query("SELECT DISTINCT c.category FROM Course c WHERE c.category IS NOT NULL AND c.category <> ''")
    List<String> findDistinctCategories();

    // ✅ PHƯƠNG THỨC TÌM KIẾM MỚI CHO ADMIN (Tìm kiếm theo Title hoặc Description)
    @Query("SELECT c FROM Course c WHERE LOWER(c.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Course> searchCoursesByTerm(@Param("searchTerm") String searchTerm, Pageable pageable);
}