package com.forsakenecho.learning_management_system.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forsakenecho.learning_management_system.entity.TransactionHistory;
import com.forsakenecho.learning_management_system.entity.User;
import com.forsakenecho.learning_management_system.enums.TransactionType;
import com.forsakenecho.learning_management_system.repository.TransactionHistoryRepository;
import com.forsakenecho.learning_management_system.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final UserRepository userRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final ObjectMapper objectMapper;

    @Value("${stripe.secretKey}")
    private String secretKey;

    @Value("${stripe.webhookSecret}")
    private String webhookSecret;

    @PostMapping("/stripe-webhook")
    @Transactional
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Stripe.apiKey = secretKey;

        try {
            // Xác thực chữ ký webhook từ Stripe
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            System.out.println("📩 Received event: " + event.getType());

            // Xử lý khi thanh toán thành công
            if ("checkout.session.completed".equals(event.getType())) {
                JsonNode jsonNode = objectMapper.readTree(payload).path("data").path("object");

                String paymentStatus = jsonNode.get("payment_status").asText();
                String currency = jsonNode.get("currency").asText();
                long amountTotal = jsonNode.get("amount_total").asLong();
                String userIdStr = jsonNode.path("metadata").path("userId").asText();
                String sessionId = jsonNode.get("id").asText();

                System.out.println("✅ Session ID: " + sessionId);
                System.out.println("💰 Amount: " + amountTotal + " " + currency);
                System.out.println("📌 Payment Status: " + paymentStatus);
                System.out.println("👤 User ID from metadata: " + userIdStr);

                if (!"paid".equalsIgnoreCase(paymentStatus)) {
                    System.out.println("⚠ Payment not paid, skipping...");
                    return ResponseEntity.ok("Payment not paid");
                }
                if (userIdStr == null || userIdStr.isEmpty()) {
                    System.err.println("❌ Missing userId in metadata");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing userId");
                }

                UUID userId = UUID.fromString(userIdStr);
                BigDecimal amount;
                if ("vnd".equalsIgnoreCase(currency)) {
                    amount = BigDecimal.valueOf(amountTotal);
                } else {
                    amount = BigDecimal.valueOf(amountTotal).divide(BigDecimal.valueOf(100));
                }

                // Cộng tiền vào tài khoản
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found: " + userId));

                user.setBalance(user.getBalance().add(amount));
                userRepository.save(user);

                // Lưu lịch sử giao dịch
                transactionHistoryRepository.save(TransactionHistory.builder()
                        .user(user)
                        .type(TransactionType.TOP_UP)
                        .amount(amount)
                        .description("Nạp tiền qua Stripe - Session ID: " + sessionId)
                        .build());

                System.out.println("✅ Nạp tiền thành công cho " + user.getEmail() + ": +" + amount + " " + currency);
            }

        } catch (SignatureVerificationException e) {
            System.err.println("❌ Invalid Stripe signature: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook handling error");
        }

        return ResponseEntity.ok("Success");
    }
}
