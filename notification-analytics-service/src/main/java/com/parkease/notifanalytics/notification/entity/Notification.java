package com.parkease.notifanalytics.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_recipient", columnList = "recipientId"),
    @Index(name = "idx_recipient_read", columnList = "recipientId, isRead"),
    @Index(name = "idx_related", columnList = "relatedId")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    @Column(nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotifType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Channel channel;

    private Long relatedId;

    @Column(length = 50)
    private String relatedType;

    @Column(nullable = false)
    @Builder.Default
    private boolean isRead = false;

    @CreationTimestamp
    private LocalDateTime sentAt;

    public enum NotifType { BOOKING_CONFIRMED, CHECK_IN, EXPIRY_REMINDER, CHECKOUT, PAYMENT_RECEIPT, PROMO, LOT_APPROVED, LOT_REJECTED }
    public enum Channel { APP, EMAIL, SMS }
}
