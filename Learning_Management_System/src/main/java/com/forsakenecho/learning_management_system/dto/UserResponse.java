package com.forsakenecho.learning_management_system.dto;

import com.forsakenecho.learning_management_system.enums.Role;
import lombok.*;

import java.util.UUID;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private String name;
    private String email;
    private Role role;



}
