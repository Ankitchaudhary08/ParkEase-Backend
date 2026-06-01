package com.parkease.notifanalytics.notification.service;

import com.parkease.notifanalytics.notification.entity.Notification;
import com.parkease.notifanalytics.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository notifRepo;
    @Mock JavaMailSender mailSender;

    @InjectMocks NotificationService notificationService;

    Notification savedNotif;

    @BeforeEach
    void setUp() {
        savedNotif = Notification.builder()
                .notificationId(1L).recipientId(10L)
                .type(Notification.NotifType.BOOKING_CONFIRMED)
                .title("Booking Confirmed").message("Spot reserved.")
                .channel(Notification.Channel.APP)
                .isRead(false).build();
    }

    // ── send ─────────────────────────────────────────────────────────────────

    @Test
    void send_createsNotificationWithCorrectFields_andSaves() {
        given(notifRepo.save(any(Notification.class))).willReturn(savedNotif);

        Notification result = notificationService.send(
                10L, Notification.NotifType.BOOKING_CONFIRMED,
                "Booking Confirmed", "Spot reserved.",
                Notification.Channel.APP, 100L, "BOOKING");

        assertThat(result.getRecipientId()).isEqualTo(10L);
        assertThat(result.isRead()).isFalse();
        then(notifRepo).should().save(argThat(n ->
                n.getRecipientId().equals(10L) &&
                !n.isRead() &&
                n.getType() == Notification.NotifType.BOOKING_CONFIRMED));
    }

    // ── sendBulk ─────────────────────────────────────────────────────────────

    @Test
    void sendBulk_createsOneNotificationPerRecipient() {
        given(notifRepo.save(any(Notification.class))).willReturn(savedNotif);

        notificationService.sendBulk(List.of(1L, 2L, 3L), "Promo Title", "50% off today");

        then(notifRepo).should(times(3)).save(any(Notification.class));
    }

    @Test
    void sendBulk_usesPromoType_forAllNotifications() {
        given(notifRepo.save(any(Notification.class))).willReturn(savedNotif);

        notificationService.sendBulk(List.of(5L, 6L), "Sale", "Big sale!");

        then(notifRepo).should(times(2)).save(
                argThat(n -> n.getType() == Notification.NotifType.PROMO));
    }

    // ── markAsRead ───────────────────────────────────────────────────────────

    @Test
    void markAsRead_setsReadFlagTrue_andSaves() {
        Notification unread = Notification.builder()
                .notificationId(2L).isRead(false).build();

        given(notifRepo.findById(2L)).willReturn(Optional.of(unread));
        given(notifRepo.save(any(Notification.class))).willAnswer(inv -> inv.getArgument(0));

        notificationService.markAsRead(2L);

        assertThat(unread.isRead()).isTrue();
        then(notifRepo).should().save(unread);
    }

    @Test
    void markAsRead_doesNothing_whenNotificationNotFound() {
        given(notifRepo.findById(999L)).willReturn(Optional.empty());

        assertThatCode(() -> notificationService.markAsRead(999L))
                .doesNotThrowAnyException();

        then(notifRepo).should(never()).save(any());
    }

    // ── markAllRead ──────────────────────────────────────────────────────────

    @Test
    void markAllRead_marksAllUnreadNotificationsAsRead() {
        Notification n1 = Notification.builder().notificationId(1L).isRead(false).build();
        Notification n2 = Notification.builder().notificationId(2L).isRead(false).build();

        given(notifRepo.findByRecipientIdAndIsRead(10L, false))
                .willReturn(List.of(n1, n2));
        given(notifRepo.save(any(Notification.class))).willAnswer(inv -> inv.getArgument(0));

        notificationService.markAllRead(10L);

        assertThat(n1.isRead()).isTrue();
        assertThat(n2.isRead()).isTrue();
        then(notifRepo).should(times(2)).save(any(Notification.class));
    }

    @Test
    void markAllRead_doesNothing_whenNoUnreadNotifications() {
        given(notifRepo.findByRecipientIdAndIsRead(10L, false)).willReturn(List.of());

        notificationService.markAllRead(10L);

        then(notifRepo).should(never()).save(any());
    }

    // ── getByRecipient ───────────────────────────────────────────────────────

    @Test
    void getByRecipient_returnsNotificationsFromRepository() {
        given(notifRepo.findByRecipientIdOrderBySentAtDesc(10L))
                .willReturn(List.of(savedNotif));

        List<Notification> result = notificationService.getByRecipient(10L);

        assertThat(result).hasSize(1)
                .first().extracting(Notification::getRecipientId).isEqualTo(10L);
    }

    // ── getUnreadCount ───────────────────────────────────────────────────────

    @Test
    void getUnreadCount_returnsCountFromRepository() {
        given(notifRepo.countByRecipientIdAndIsRead(10L, false)).willReturn(4);

        assertThat(notificationService.getUnreadCount(10L)).isEqualTo(4);
    }

    @Test
    void getUnreadCount_returnsZero_whenAllRead() {
        given(notifRepo.countByRecipientIdAndIsRead(10L, false)).willReturn(0);

        assertThat(notificationService.getUnreadCount(10L)).isEqualTo(0);
    }

    // ── sendEmail ────────────────────────────────────────────────────────────

    @Test
    void sendEmail_sendsCorrectMessageViaMailSender() {
        notificationService.sendEmail("user@test.com", "Test Subject", "Test body text");

        then(mailSender).should().send(argThat((SimpleMailMessage msg) ->
                "user@test.com".equals(msg.getTo()[0]) &&
                "Test Subject".equals(msg.getSubject()) &&
                "Test body text".equals(msg.getText())));
    }

    @Test
    void sendEmail_doesNotThrowException_whenMailSenderFails() {
        doThrow(new RuntimeException("SMTP connection refused"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> notificationService.sendEmail("bad@test.com", "Sub", "Body"))
                .doesNotThrowAnyException();
    }

    // ── handleBookingEvent ───────────────────────────────────────────────────

    @Test
    void handleBookingEvent_RESERVED_sendsBookingConfirmedNotification() {
        given(notifRepo.save(any(Notification.class))).willReturn(savedNotif);

        notificationService.handleBookingEvent(Map.of(
                "status", "RESERVED", "userId", 10L, "bookingId", 100L));

        then(notifRepo).should().save(argThat(n ->
                n.getType() == Notification.NotifType.BOOKING_CONFIRMED &&
                n.getTitle().equals("Booking Confirmed!")));
    }

    @Test
    void handleBookingEvent_ACTIVE_sendsCheckInNotification() {
        given(notifRepo.save(any(Notification.class))).willReturn(savedNotif);

        notificationService.handleBookingEvent(Map.of(
                "status", "ACTIVE", "userId", 10L, "bookingId", 100L));

        then(notifRepo).should().save(
                argThat(n -> n.getType() == Notification.NotifType.CHECK_IN));
    }

    @Test
    void handleBookingEvent_COMPLETED_sendsCheckOutNotification_withTotalFare() {
        given(notifRepo.save(any(Notification.class))).willReturn(savedNotif);

        notificationService.handleBookingEvent(Map.of(
                "status", "COMPLETED", "userId", 10L, "bookingId", 100L, "totalAmount", 150.0));

        then(notifRepo).should().save(argThat(n ->
                n.getType() == Notification.NotifType.CHECKOUT &&
                n.getMessage().contains("150.00")));
    }

    @Test
    void handleBookingEvent_CANCELLED_sendsCancelledNotification() {
        given(notifRepo.save(any(Notification.class))).willReturn(savedNotif);

        notificationService.handleBookingEvent(Map.of(
                "status", "CANCELLED", "userId", 10L, "bookingId", 100L));

        then(notifRepo).should().save(argThat(n ->
                n.getTitle().equals("Booking Cancelled")));
    }

    @Test
    void handleBookingEvent_unknownStatus_doesNotSaveAnyNotification() {
        notificationService.handleBookingEvent(Map.of(
                "status", "UNKNOWN_STATUS", "userId", 10L, "bookingId", 100L));

        then(notifRepo).should(never()).save(any());
    }

    @Test
    void handleBookingEvent_doesNotThrow_onMalformedPayload() {
        assertThatCode(() -> notificationService.handleBookingEvent(Map.of()))
                .doesNotThrowAnyException();
    }

    // ── handlePaymentEvent ───────────────────────────────────────────────────

    @Test
    void handlePaymentEvent_PAID_sendsPaymentSuccessfulNotification() {
        given(notifRepo.save(any(Notification.class))).willReturn(savedNotif);

        notificationService.handlePaymentEvent(Map.of(
                "userId", 10L, "paymentId", 5L,
                "amount", 300.0, "status", "PAID",
                "transactionId", "TXN-001"));

        then(notifRepo).should().save(argThat(n ->
                n.getType() == Notification.NotifType.PAYMENT_RECEIPT &&
                n.getTitle().equals("Payment Successful") &&
                n.getMessage().contains("300.00")));
    }

    @Test
    void handlePaymentEvent_REFUNDED_sendsRefundNotification() {
        given(notifRepo.save(any(Notification.class))).willReturn(savedNotif);

        notificationService.handlePaymentEvent(Map.of(
                "userId", 10L, "paymentId", 5L,
                "amount", 300.0, "status", "REFUNDED"));

        then(notifRepo).should().save(argThat(n ->
                n.getTitle().equals("Refund Processed") &&
                n.getMessage().contains("300.00")));
    }

    @Test
    void handlePaymentEvent_otherStatus_doesNotSaveNotification() {
        notificationService.handlePaymentEvent(Map.of(
                "userId", 10L, "paymentId", 5L,
                "amount", 100.0, "status", "FAILED"));

        then(notifRepo).should(never()).save(any());
    }

    @Test
    void handlePaymentEvent_doesNotThrow_onMalformedPayload() {
        assertThatCode(() -> notificationService.handlePaymentEvent(Map.of()))
                .doesNotThrowAnyException();
    }
}
