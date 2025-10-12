package com.forsakenecho.learning_management_system.repository;

import com.forsakenecho.learning_management_system.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    Page<Event> findAllByOrderByTimestampDesc(Pageable pageable);
    List<Event> findAllByOrderByTimestampDesc();

    // ✅ PHƯƠNG THỨC TÌM KIẾM MỚI CHO ADMIN (Tìm kiếm theo Action hoặc PerformedBy)
    @Query("SELECT e FROM Event e WHERE LOWER(e.action) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(e.performedBy) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY e.timestamp DESC")
    Page<Event> searchEventsByTerm(@Param("searchTerm") String searchTerm, Pageable pageable);
}
