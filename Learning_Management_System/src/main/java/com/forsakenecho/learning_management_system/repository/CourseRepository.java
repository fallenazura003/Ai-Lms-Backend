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

    // ✅ Phương thức findVisibleCoursesByKeyword này HIỆN TẠI KHÔNG CẦN THIẾT NỮA
    // nếu bạn đã chuyển sang dùng Specification trong CourseService.
    // Bạn có thể giữ nó nếu muốn giữ lại khả năng tìm kiếm cơ bản này ngoài Specification,
    // nhưng để tránh nhầm lẫn, tôi sẽ chú thích nó.
    // @Query("""
    // SELECT c FROM Course c
    // WHERE c.visible = true AND (
    //     LOWER(c.title) LIKE %:keyword% OR
    //     LOWER(c.description) LIKE %:keyword% OR
    //     LOWER(c.creator.name) LIKE %:keyword%
    // )
    // """)
    // Page<Course> findVisibleCoursesByKeyword(@Param("keyword") String keyword, Pageable pageable);


    // ✅ PHƯƠNG THỨC MỚI: Để lấy danh sách các danh mục duy nhất cho bộ lọc.
    // Điều này vẫn cần thiết vì Specification không có sẵn chức năng DISTINCT cho một trường cụ thể như thế này.
    @Query("SELECT DISTINCT c.category FROM Course c WHERE c.category IS NOT NULL AND c.category <> ''")
    List<String> findDistinctCategories();
}