package com.forsakenecho.learning_management_system.dto;

import com.forsakenecho.learning_management_system.enums.Role;
import lombok.Data;

@Data
public class RegisterRequest {
    private String name;
    private String email;
    private String password;
    private Role role;
}
