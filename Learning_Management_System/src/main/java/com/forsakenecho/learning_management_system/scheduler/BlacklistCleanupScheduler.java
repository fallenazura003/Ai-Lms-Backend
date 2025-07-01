package com.forsakenecho.learning_management_system.scheduler;

import com.forsakenecho.learning_management_system.repository.BlacklistedTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class BlacklistCleanupScheduler {

    private final BlacklistedTokenRepository blacklistedTokenRepository;

    // Chạy mỗi giờ (3600000 ms) hoặc tùy chỉnh theo nhu cầu của bạn
    // Cron expression ví dụ: "0 0 * * * *" nghĩa là vào phút 0, giờ 0 của mỗi giờ, mỗi ngày
    @Scheduled(fixedRate = 3600000) // Chạy mỗi giờ
    @Transactional // Đảm bảo giao dịch cho thao tác xóa DB
    public void cleanupBlacklistedTokens() {
        LocalDateTime now = LocalDateTime.now();
        // Xóa tất cả các token có expiration_time đã qua
        // Bạn sẽ cần một phương thức tùy chỉnh trong repository hoặc dùng JPA Query
        // Ví dụ:
        // blacklistedTokenRepository.deleteByExpirationTimeBefore(now);
        // Hoặc viết một native query trong repo:
        // @Modifying @Query("DELETE FROM blacklisted_tokens bt WHERE bt.expiration_time < :now")
        // void deleteExpiredTokens(@Param("now") LocalDateTime now);

        // Cách đơn giản hơn nếu không muốn thêm method vào repo:
        // Lấy tất cả và lọc rồi xóa, nhưng không hiệu quả cho DB lớn
        blacklistedTokenRepository.findAll().forEach(token -> {
            if (token.getExpirationTime().isBefore(now)) {
                blacklistedTokenRepository.delete(token);
            }
        });
        System.out.println("Cleaned up expired blacklisted tokens at " + now);
    }
}