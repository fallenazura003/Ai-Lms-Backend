package com.forsakenecho.learning_management_system.config;

import com.forsakenecho.learning_management_system.jwt.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod; // ✅ Thêm import này
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final UserDetailsService userDetailsService;

    @Order(1) // Chain cho Stripe webhook, không add JwtFilter
    @Bean
    public SecurityFilterChain webhookFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/payment/stripe-webhook")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    @Order(3)
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors
                        .configurationSource(request -> {
                            var config = new org.springframework.web.cors.CorsConfiguration();
                            config.setAllowedOrigins(List.of("http://localhost:3000"));
                            config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                            config.setAllowedHeaders(List.of("*"));
                            config.setAllowCredentials(true);
                            config.setMaxAge(3600L);
                            return config;
                        })
                )
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        // ✅ RẤT QUAN TRỌNG: Cho phép tất cả các yêu cầu OPTIONS đi qua mà không cần xác thực
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // Thêm dòng này

                        .requestMatchers("/api/auth/**").permitAll()

                        .requestMatchers("/api/payment/**").permitAll()

                        .requestMatchers("/api/teacher/courses/generate").permitAll()
                        .requestMatchers("/api/teacher/ai/generate-lesson").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/student/courses/{courseId}/comments").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/student/courses/{courseId}/comments").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/student/courses/{courseId}/comments/{commentId}").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/student/courses/{courseId}/comments/{commentId}").permitAll()




                        .requestMatchers(HttpMethod.GET, "/api/student/courses/purchased-ids/{courseId}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/student/courses/{courseId}/rating/average").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/notifications/mark-all-as-read").hasAnyRole("STUDENT", "TEACHER", "ADMIN")


                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/teacher/**").hasRole("TEACHER")
                        .requestMatchers("/api/student/**").hasRole("STUDENT")

                        .requestMatchers("/uploads/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(sess ->
                        sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
}