package com.forsakenecho.learning_management_system.controller;

import com.forsakenecho.learning_management_system.dto.*;
import com.forsakenecho.learning_management_system.entity.BlacklistedToken;
import com.forsakenecho.learning_management_system.entity.User;
import com.forsakenecho.learning_management_system.enums.Status;
import com.forsakenecho.learning_management_system.jwt.JwtUtil;
import com.forsakenecho.learning_management_system.repository.BlacklistedTokenRepository;
import com.forsakenecho.learning_management_system.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@RequestMapping("/api/auth")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final BlacklistedTokenRepository blacklistedTokenRepository;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        User user = User.builder()
                .name(registerRequest.getName())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role(registerRequest.getRole())
                .status(Status.valueOf("ACTIVE"))
                .balance(BigDecimal.ZERO)
                .build();

        userRepository.save(user);

        UserResponse userResponse = UserResponse.builder()
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();

        ApiResponse<UserResponse> response = ApiResponse.<UserResponse>builder()
                .message("Tạo tài khoản thành công!")
                .data(userResponse)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) throws Exception {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );

        User user = userRepository.findByEmail(loginRequest.getEmail()).orElseThrow();
        String token = jwtUtil.generateToken(user);

        // ✅ Thêm userId vào response
        return ResponseEntity.ok(new AuthResponse(
                token,
                user.getRole().name(),
                user.getStatus().name(),
                user.getId().toString(), // gửi UUID dưới dạng chuỗi
                user.getName(),
                user.getEmail()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // 1. Kiểm tra nếu token đã hết hạn (từ phía server)
            // Nếu token đã hết hạn, không cần thêm vào blacklist
            try {
                if (jwtUtil.isTokenExpired(token)) {
                    return ResponseEntity.ok("Token already expired. Logged out successfully.");
                }
            } catch (Exception e) {
                // Xử lý các lỗi khi parse token (malformed, signature error, etc.)
                // Nếu token lỗi, cũng không cần blacklist
                return ResponseEntity.badRequest().body("Invalid token format or signature. Unable to process logout.");
            }


            // 2. Lấy thời gian hết hạn của token từ JWT
            Date expirationDate = jwtUtil.extractExpiration(token);
            LocalDateTime expirationTime = expirationDate.toInstant()
                    .atZone(ZoneId.systemDefault()) // Hoặc ZoneId.of("UTC")
                    .toLocalDateTime();

            // 3. Thêm token vào blacklist (sử dụng hash token)
            String tokenToBlacklist = jwtUtil.hashToken(token);

            BlacklistedToken blacklistedEntry = BlacklistedToken.builder()
                    .tokenHash(tokenToBlacklist)
                    .expirationTime(expirationTime)
                    .build();

            try {
                blacklistedTokenRepository.save(blacklistedEntry);
                return ResponseEntity.ok("Logged out successfully");
            } catch (Exception e) {
                // Xử lý trường hợp token đã có trong blacklist (ví dụ: người dùng logout 2 lần)
                System.err.println("Error blacklisting token: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to blacklist token");
            }

        }
        return ResponseEntity.badRequest().body("Missing or invalid Authorization header");
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        return ResponseEntity.ok(auth.getPrincipal());
    }
}
