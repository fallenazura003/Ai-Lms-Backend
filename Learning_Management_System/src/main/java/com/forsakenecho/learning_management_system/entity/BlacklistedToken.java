package com.forsakenecho.learning_management_system.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "blacklisted_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlacklistedToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // IDENTITY cho khóa chính tự động tăng của MySQL
    private Long id;

    @Column(name = "token_hash", unique = true, nullable = false)
    private String tokenHash; // Sẽ lưu trữ hash của token hoặc toàn bộ token

    @Column(name = "expiration_time", nullable = false)
    private LocalDateTime expirationTime; // Thời điểm token gốc hết hạn

    @Column(name = "blacklisted_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime blacklistedAt;

    // <-- THÊM PHƯƠNG THỨC NÀY
    @PrePersist
    protected void onCreate() {
        if (blacklistedAt == null) { // Chỉ thiết lập nếu chưa có giá trị
            blacklistedAt = LocalDateTime.now();
        }
    }
}