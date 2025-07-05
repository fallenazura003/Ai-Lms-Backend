package com.forsakenecho.learning_management_system.repository;

import com.forsakenecho.learning_management_system.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RatingRepository extends JpaRepository<Rating, UUID> {
    Optional<Rating> findByCourse_IdAndStudent_Id(UUID courseId, UUID studentId);
    List<Rating> findByCourse_Id(UUID courseId);

    @Query("SELECT AVG(r.value) FROM Rating r WHERE r.course.id = :courseId")
    Double getAverageRating(UUID courseId);
}