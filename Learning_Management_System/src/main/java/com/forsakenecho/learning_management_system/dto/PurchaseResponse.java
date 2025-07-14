package com.forsakenecho.learning_management_system.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseResponse {
    private UUID courseId;
    private UUID studentId;
    private LocalDateTime purchasedAt;
    private BigDecimal balance;
}
