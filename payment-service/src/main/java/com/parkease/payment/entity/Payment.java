package com.parkease.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_booking", columnList = "bookingId"),
    @Index(name = "idx_user", columnList = "userId"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_paid_at", columnList = "paidAt")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @Column(nullable = false)
    private Long bookingId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMode mode;

    @Column(length = 255)
    private String transactionId;

    @Column(length = 255)
    private String razorpayOrderId;

    @Column(length = 255)
    private String razorpayPaymentId;

    @Column(length = 10)
    @Builder.Default
    private String currency = "INR";

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;

    @Column(length = 500)
    private String description;

    @Column(length = 512)
    private String receiptUrl;

    public enum PaymentStatus { PENDING, PAID, REFUNDED, FAILED }
    public enum PaymentMode { CARD, UPI, WALLET, CASH }
}
