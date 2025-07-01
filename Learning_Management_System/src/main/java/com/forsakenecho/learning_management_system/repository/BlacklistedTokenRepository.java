package com.forsakenecho.learning_management_system.repository;

import com.forsakenecho.learning_management_system.entity.BlacklistedToken; // Import entity
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {

    // Phương thức để kiểm tra xem một token_hash có tồn tại trong blacklist không
    boolean existsByTokenHash(String tokenHash);

    // Phương thức để tìm token đã hết hạn để dọn dẹp
    // Bạn có thể cần thêm query cụ thể nếu muốn tìm tất cả các token đã hết hạn
    // List<BlacklistedToken> findByExpirationTimeBefore(LocalDateTime dateTime);
}