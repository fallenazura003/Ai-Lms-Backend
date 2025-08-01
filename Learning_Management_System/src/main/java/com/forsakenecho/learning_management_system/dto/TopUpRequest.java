package com.forsakenecho.learning_management_system.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TopUpRequest {
    private BigDecimal amount;
    private String currency = "VND";

}