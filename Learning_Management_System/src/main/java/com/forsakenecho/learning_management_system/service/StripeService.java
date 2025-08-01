package com.forsakenecho.learning_management_system.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class StripeService {

    @Value("${stripe.secretKey}")
    private String secretKey;

    @Value("${stripe.successUrl}")
    private String successUrl;

    @Value("${stripe.cancelUrl}")
    private String cancelUrl;

    public String createCheckoutSession(BigDecimal amount, String currency, UUID userId) throws StripeException {
        Stripe.apiKey = secretKey;

        // Stripe yêu cầu số tiền là long (đơn vị cent)
        long amountLong = amount.longValue();

        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(currency)
                                                .setUnitAmount(amountLong)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Nạp tiền vào ví")
                                                                .build())
                                                .build())
                                .setQuantity(1L)
                                .build())
                // Thêm metadata để bạn có thể xác định người dùng sau này trong webhook
                .putMetadata("userId", userId.toString())
                .putMetadata("transactionType", "TOP_UP")
                .build();

        Session session = Session.create(params);
        return session.getUrl();
    }
}