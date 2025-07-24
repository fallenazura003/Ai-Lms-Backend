package com.forsakenecho.learning_management_system.repository;

import com.forsakenecho.learning_management_system.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByCourseIdOrderByCreatedAtAsc(UUID courseId);

    List<Comment>  findByCourseId(UUID courseId);
    Optional<Comment> findByIdAndAuthor_Id(UUID id, UUID authorId);

    void deleteByCourse_Id(UUID courseId);
}


