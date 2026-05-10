package com.parkease.payment.messaging;

import com.parkease.payment.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public static final String PAYMENT_EXCHANGE = "parkease.payment.exchange";
    public static final String PAYMENT_COMPLETED_KEY = "payment.completed";
    public static final String PAYMENT_REFUNDED_KEY = "payment.refunded";

    public void publishPaymentCompleted(Payment payment) {
        rabbitTemplate.convertAndSend(PAYMENT_EXCHANGE, PAYMENT_COMPLETED_KEY, toPayload(payment));
    }

    public void publishPaymentRefunded(Payment payment) {
        rabbitTemplate.convertAndSend(PAYMENT_EXCHANGE, PAYMENT_REFUNDED_KEY, toPayload(payment));
    }

    private Map<String, Object> toPayload(Payment payment) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentId", payment.getPaymentId());
        payload.put("bookingId", payment.getBookingId());
        payload.put("userId", payment.getUserId());
        payload.put("amount", payment.getAmount());
        payload.put("status", payment.getStatus().name());
        payload.put("mode", payment.getMode().name());
        payload.put("transactionId", payment.getTransactionId());
        payload.put("paidAt", payment.getPaidAt() != null ? payment.getPaidAt().toString() : null);
        payload.put("receiptUrl", payment.getReceiptUrl());
        return payload;
    }
}
