package com.forsakenecho.learning_management_system.dto;

import com.forsakenecho.learning_management_system.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String role;
    private String status;
}
