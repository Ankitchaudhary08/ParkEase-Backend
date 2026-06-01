package com.parkease.notifanalytics.notification.service;

import com.parkease.notifanalytics.notification.entity.Notification;
import com.parkease.notifanalytics.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notifRepo;
    private final JavaMailSender mailSender;

    @RabbitListener(queues = "parkease.notification.queue")
    public void handleBookingEvent(Map<String, Object> payload) {
        try {
            String status = (String) payload.get("status");
            Long userId = Long.valueOf(payload.get("userId").toString());
            Long bookingId = Long.valueOf(payload.get("bookingId").toString());

            Notification.NotifType type;
            String title;
            String message;

            switch (status) {
                case "RESERVED" -> {
                    type = Notification.NotifType.BOOKING_CONFIRMED;
                    title = "Booking Confirmed!";
                    message = "Your spot has been reserved. Booking ID: " + bookingId;
                }
                case "ACTIVE" -> {
                    type = Notification.NotifType.CHECK_IN;
                    title = "Checked In Successfully";
                    message = "You have checked in. Have a great parking experience!";
                }
                case "COMPLETED" -> {
                    type = Notification.NotifType.CHECKOUT;
                    title = "Checked Out";
                    message = String.format("You checked out. Total fare: ₹%.2f",
                            payload.get("totalAmount") != null ? Double.parseDouble(payload.get("totalAmount").toString()) : 0.0);
                }
                case "CANCELLED" -> {
                    type = Notification.NotifType.BOOKING_CONFIRMED;
                    title = "Booking Cancelled";
                    message = "Your booking #" + bookingId + " has been cancelled.";
                }
                default -> {
                    log.warn("Unknown booking status for notification: {}", status);
                    return;
                }
            }

            send(userId, type, title, message, Notification.Channel.APP, bookingId, "BOOKING");
        } catch (Exception e) {
            log.error("Failed to process booking notification event: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = "parkease.payment.notification.queue")
    public void handlePaymentEvent(Map<String, Object> payload) {
        try {
            Long userId = Long.valueOf(payload.get("userId").toString());
            Long paymentId = Long.valueOf(payload.get("paymentId").toString());
            Double amount = Double.parseDouble(payload.get("amount").toString());
            String status = (String) payload.get("status");

            if ("PAID".equals(status)) {
                send(userId, Notification.NotifType.PAYMENT_RECEIPT,
                        "Payment Successful",
                        String.format("Payment of ₹%.2f confirmed. Transaction ID: %s", amount, payload.get("transactionId")),
                        Notification.Channel.APP, paymentId, "PAYMENT");
            } else if ("REFUNDED".equals(status)) {
                send(userId, Notification.NotifType.PAYMENT_RECEIPT,
                        "Refund Processed",
                        String.format("Refund of ₹%.2f has been processed to your original payment method.", amount),
                        Notification.Channel.APP, paymentId, "PAYMENT");
            }
        } catch (Exception e) {
            log.error("Failed to process payment notification event: {}", e.getMessage());
        }
    }

    public Notification send(Long recipientId, Notification.NotifType type, String title, String message,
                              Notification.Channel channel, Long relatedId, String relatedType) {
        Notification notif = Notification.builder()
                .recipientId(recipientId)
                .type(type)
                .title(title)
                .message(message)
                .channel(channel)
                .relatedId(relatedId)
                .relatedType(relatedType)
                .isRead(false)
                .build();
        return notifRepo.save(notif);
    }

    public void sendBulk(List<Long> recipientIds, String title, String message) {
        recipientIds.forEach(id -> send(id, Notification.NotifType.PROMO, title, message, Notification.Channel.APP, null, null));
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notifRepo.findById(notificationId).ifPresent(n -> { n.setRead(true); notifRepo.save(n); });
    }

    @Transactional
    public void markAllRead(Long recipientId) {
        notifRepo.findByRecipientIdAndIsRead(recipientId, false).forEach(n -> { n.setRead(true); notifRepo.save(n); });
    }

    public List<Notification> getByRecipient(Long recipientId) {
        return notifRepo.findByRecipientIdOrderBySentAtDesc(recipientId);
    }

    public int getUnreadCount(Long recipientId) {
        return notifRepo.countByRecipientIdAndIsRead(recipientId, false);
    }

    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
