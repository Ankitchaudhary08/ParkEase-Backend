package com.parkease.payment.service;

import com.parkease.payment.dto.RazorpayOrderRequest;
import com.parkease.payment.dto.RazorpayOrderResponse;
import com.parkease.payment.dto.RazorpayVerifyRequest;
import com.parkease.payment.entity.Payment;
import com.parkease.payment.messaging.PaymentEventPublisher;
import com.parkease.payment.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class RazorpayService {

    private final RazorpayClient razorpayClient;
    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    /**
     * Creates a Razorpay order for UPI / card checkout.
     * Amount must be in INR; Razorpay expects paise (multiply by 100).
     */
    public RazorpayOrderResponse createOrder(RazorpayOrderRequest request) {
        try {
            JSONObject orderRequest = new JSONObject();
            long amountInPaise = Math.round(request.getAmount() * 100);
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", request.getCurrency());
            orderRequest.put("receipt", "BOOKING-" + request.getBookingId());
            orderRequest.put("payment_capture", 1);

            JSONObject notes = new JSONObject();
            notes.put("bookingId", request.getBookingId().toString());
            notes.put("userId", request.getUserId().toString());
            if (request.getDescription() != null) {
                notes.put("description", request.getDescription());
            }
            orderRequest.put("notes", notes);

            Order order = razorpayClient.orders.create(orderRequest);
            String rzpOrderId = order.get("id");

            log.info("Razorpay order created: {} for booking: {}", rzpOrderId, request.getBookingId());

            return RazorpayOrderResponse.builder()
                    .razorpayOrderId(rzpOrderId)
                    .amountInPaise(amountInPaise)
                    .currency(request.getCurrency())
                    .keyId(razorpayKeyId)
                    .bookingId(request.getBookingId())
                    .userId(request.getUserId())
                    .description(request.getDescription())
                    .build();

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order for booking {}: {}", request.getBookingId(), e.getMessage());
            throw new RuntimeException("Razorpay order creation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Verifies Razorpay payment signature using HMAC-SHA256.
     * Signature = HMAC_SHA256(razorpayOrderId + "|" + razorpayPaymentId, keySecret)
     * On success, persists a PAID Payment record and publishes the payment.completed event.
     */
    @Transactional
    public Payment verifyAndCapture(RazorpayVerifyRequest request) {
        String expectedSignature = computeHmacSha256(
                request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId(),
                razorpayKeySecret
        );

        if (!expectedSignature.equals(request.getRazorpaySignature())) {
            log.warn("Razorpay signature mismatch for order: {}", request.getRazorpayOrderId());
            throw new IllegalArgumentException("Payment signature verification failed");
        }

        Payment payment = paymentRepository.findByBookingId(request.getBookingId())
                .orElse(Payment.builder()
                        .bookingId(request.getBookingId())
                        .userId(request.getUserId())
                        .amount(request.getAmount())
                        .mode(Payment.PaymentMode.UPI)
                        .currency("INR")
                        .build());

        payment.setMode(Payment.PaymentMode.UPI);
        payment.setStatus(Payment.PaymentStatus.PAID);
        payment.setRazorpayOrderId(request.getRazorpayOrderId());
        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setTransactionId(request.getRazorpayPaymentId());
        payment.setPaidAt(LocalDateTime.now());
        payment.setAmount(request.getAmount());

        Payment saved = paymentRepository.save(payment);
        eventPublisher.publishPaymentCompleted(saved);

        log.info("Payment verified and captured. PaymentId: {}, RazorpayId: {}",
                saved.getPaymentId(), saved.getRazorpayPaymentId());

        return saved;
    }

    private String computeHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC computation failed", e);
        }
    }
}
