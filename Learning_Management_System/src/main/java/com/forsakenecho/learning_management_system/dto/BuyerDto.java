// src/main/java/com/forsakenecho/learning_management_system/dto/BuyerDto.java
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
public class BuyerDto {
    private UUID userId;
    private String userName;
    private String userEmail;
    private LocalDateTime purchaseDate;
}