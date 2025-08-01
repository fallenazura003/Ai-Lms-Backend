package com.forsakenecho.learning_management_system.controller;

import com.forsakenecho.learning_management_system.entity.TransactionHistory;
import com.forsakenecho.learning_management_system.entity.User;
import com.forsakenecho.learning_management_system.enums.TransactionType;
import com.forsakenecho.learning_management_system.repository.TransactionHistoryRepository;
import com.forsakenecho.learning_management_system.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
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

    @Value("${stripe.secretKey}")
    private String secretKey;

    @Value("${stripe.webhookSecret}")
    private String webhookSecret;

    @PostMapping("/stripe-webhook")
    @Transactional
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        Stripe.apiKey = secretKey;
        Event event;

        try {
            // Xác minh chữ ký webhook để đảm bảo sự kiện đến từ Stripe
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            return new ResponseEntity<>("Chữ ký không hợp lệ.", HttpStatus.BAD_REQUEST);
        }

        // Xử lý sự kiện tùy thuộc vào loại sự kiện
        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getDataObjectDeserializer().getObject().get();

            try {
                // 🔍 Truy xuất PaymentIntent từ session
                PaymentIntent paymentIntent = PaymentIntent.retrieve(session.getPaymentIntent());

                if ("succeeded".equals(paymentIntent.getStatus())) {
                    UUID userId = UUID.fromString(session.getMetadata().get("userId"));

                    // 🔎 Sử dụng đúng đơn vị tiền tệ
                    BigDecimal amount = BigDecimal.valueOf(session.getAmountTotal())
                            .divide(BigDecimal.valueOf(1000)); // nếu là VND, Stripe trả mili-VND

                    User user = userRepository.findById(userId).orElseThrow(
                            () -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));

                    user.setBalance(user.getBalance().add(amount));
                    userRepository.save(user);

                    transactionHistoryRepository.save(TransactionHistory.builder()
                            .user(user)
                            .type(TransactionType.TOP_UP)
                            .amount(amount)
                            .description("Nạp tiền vào ví qua Stripe, Session ID: " + session.getId())
                            .build());
                }

            } catch (Exception e) {
                return new ResponseEntity<>("Lỗi xử lý giao dịch", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        return new ResponseEntity<>("Success", HttpStatus.OK);
    }
}