package com.forsakenecho.learning_management_system.service;

import com.forsakenecho.learning_management_system.entity.User;
import com.forsakenecho.learning_management_system.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StripeService {

    private final UserRepository userRepository;

    @Value("${stripe.secretKey}")
    private String secretKey;

    @Value("${stripe.successUrl}")
    private String successUrl;

    @Value("${stripe.cancelUrl}")
    private String cancelUrl;

    public String createCheckoutSession(BigDecimal amount, String currency, UUID userId) throws StripeException {
        Stripe.apiKey = secretKey;

        // Lấy user từ DB
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        // Nếu chưa có stripeCustomerId -> tạo mới
        String stripeCustomerId = user.getStripeCustomerId();
        if (stripeCustomerId == null) {
            Customer customer = Customer.create(
                    CustomerCreateParams.builder()
                            .setEmail(user.getEmail())
                            .setName(user.getUsername())
                            .build()
            );
            stripeCustomerId = customer.getId();
            user.setStripeCustomerId(stripeCustomerId);
            userRepository.save(user);
        }

        long amountLong = amount.longValue(); // VND → đồng nhỏ nhất

        // Tạo Checkout Session
        SessionCreateParams params = SessionCreateParams.builder()
                .setCustomer(stripeCustomerId) // Dùng customer cũ
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(currency.toLowerCase())
                                                .setUnitAmount(amountLong)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Nạp tiền vào ví")
                                                                .build())
                                                .build())
                                .setQuantity(1L)
                                .build())
                .putMetadata("userId", user.getId().toString()) // Metadata để webhook nhận diện user
                .putMetadata("transactionType", "TOP_UP")
                .build();

        Session session = Session.create(params);
        return session.getUrl();
    }
}
