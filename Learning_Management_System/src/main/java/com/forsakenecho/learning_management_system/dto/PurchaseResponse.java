package com.forsakenecho.learning_management_system.dto;

import lombok.*;

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
}
