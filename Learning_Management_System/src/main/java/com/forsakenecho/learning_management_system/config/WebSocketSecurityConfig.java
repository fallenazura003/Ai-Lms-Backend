package com.forsakenecho.learning_management_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSocketSecurityConfig {

    @Bean
    @Order(2) // đảm bảo filter WebSocket ưu tiên xử lý
    public SecurityFilterChain webSocketSecurity(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/ws/**") // áp dụng cho các request /ws/**
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                .csrf(csrf -> csrf.disable())
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}

