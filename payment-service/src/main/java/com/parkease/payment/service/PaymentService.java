package com.parkease.payment.service;

import com.parkease.payment.dto.ProcessPaymentRequest;
import com.parkease.payment.entity.Payment;
import com.parkease.payment.messaging.PaymentEventPublisher;
import com.parkease.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;

    @Transactional
    public Payment processPayment(ProcessPaymentRequest request) {
        Payment payment = Payment.builder()
                .bookingId(request.getBookingId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .mode(request.getMode())
                .status(Payment.PaymentStatus.PENDING)
                .description(request.getDescription())
                .build();

        if (request.getMode() == Payment.PaymentMode.CASH) {
            payment.setStatus(Payment.PaymentStatus.PENDING);
        } else {
            String txnId = request.getTransactionId() != null
                    ? request.getTransactionId()
                    : "TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
            payment.setTransactionId(txnId);
            payment.setStatus(Payment.PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
        }

        Payment saved = paymentRepository.save(payment);

        if (saved.getStatus() == Payment.PaymentStatus.PAID) {
            eventPublisher.publishPaymentCompleted(saved);
        }

        return saved;
    }

    @RabbitListener(queues = "parkease.payment.queue")
    public void handleCheckoutEvent(Map<String, Object> payload) {
        try {
            Long bookingId = Long.valueOf(payload.get("bookingId").toString());
            Long userId = Long.valueOf(payload.get("userId").toString());
            Double amount = Double.valueOf(payload.get("totalAmount").toString());

            boolean exists = paymentRepository.findByBookingId(bookingId).isPresent();
            if (!exists) {
                ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                        .bookingId(bookingId)
                        .userId(userId)
                        .amount(amount)
                        .mode(Payment.PaymentMode.CASH)
                        .description("Auto-created on checkout — awaiting settlement")
                        .build();
                processPayment(request);
                log.info("Auto-created payment record for booking {} on checkout", bookingId);
            }
        } catch (Exception e) {
            log.error("Failed to process checkout payment event: {}", e.getMessage());
        }
    }

    @Transactional
    public Payment updateStatus(Long paymentId, Payment.PaymentStatus status) {
        Payment payment = getById(paymentId);
        payment.setStatus(status);
        if (status == Payment.PaymentStatus.PAID) payment.setPaidAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment refundPayment(Long paymentId) {
        Payment payment = getById(paymentId);
        if (payment.getStatus() != Payment.PaymentStatus.PAID) {
            throw new IllegalStateException("Only PAID payments can be refunded");
        }
        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);
        eventPublisher.publishPaymentRefunded(saved);
        return saved;
    }

    public Payment getById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
    }

    public Payment getByBooking(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("No payment found for booking: " + bookingId));
    }

    public List<Payment> getByUser(Long userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Double getTotalRevenue(LocalDateTime from, LocalDateTime to) {
        return paymentRepository.sumRevenueBetween(from, to);
    }

    public String getPaymentStatus(Long paymentId) {
        return getById(paymentId).getStatus().name();
    }
}
