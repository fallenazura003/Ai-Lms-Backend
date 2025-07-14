package com.forsakenecho.learning_management_system.repository;

import com.forsakenecho.learning_management_system.entity.TransactionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, UUID> {
    Page<TransactionHistory> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

}
