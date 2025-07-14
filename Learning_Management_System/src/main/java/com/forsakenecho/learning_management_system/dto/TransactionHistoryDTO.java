package com.forsakenecho.learning_management_system.dto;

import com.forsakenecho.learning_management_system.enums.TransactionType;
import com.forsakenecho.learning_management_system.entity.TransactionHistory;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record TransactionHistoryDTO(
        TransactionType type,
        BigDecimal amount,
        String description,
        LocalDateTime createdAt
) {
    public static TransactionHistoryDTO from(TransactionHistory transaction) {
        return TransactionHistoryDTO.builder()
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
