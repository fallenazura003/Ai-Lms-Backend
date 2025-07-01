// src/main/java/com/forsakenecho/learning_management_system/dto/PurchaseDetailDto.java
package com.forsakenecho.learning_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseDetailDto {
    private UUID purchaseId;
    private UUID courseId;
    private String courseTitle;
    private String buyerName;
    private String buyerEmail;
    private double price;
    private LocalDateTime purchaseDate;
}