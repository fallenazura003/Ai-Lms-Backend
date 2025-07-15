package com.forsakenecho.learning_management_system.repository;

import com.forsakenecho.learning_management_system.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByRecipientEmailOrderByCreatedAtDesc(String email);

    // ✅ IMPORTANT: Change 'ReadFalse' to 'IsReadFalse'
    List<Notification> findByRecipientEmailAndIsReadFalse(String email);

    void deleteByCreatedAtBefore(LocalDateTime dateTime);
}