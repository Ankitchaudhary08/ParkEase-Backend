package com.parkease.payment.service;

import com.parkease.payment.dto.ProcessPaymentRequest;
import com.parkease.payment.entity.Payment;
import com.parkease.payment.messaging.PaymentEventPublisher;
import com.parkease.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock PaymentEventPublisher eventPublisher;

    @InjectMocks PaymentService paymentService;

    Payment paidPayment;

    @BeforeEach
    void setUp() {
        paidPayment = Payment.builder()
                .paymentId(1L).bookingId(10L).userId(5L)
                .amount(300.0).mode(Payment.PaymentMode.UPI)
                .status(Payment.PaymentStatus.PAID)
                .transactionId("TXN-ABCD1234")
                .paidAt(LocalDateTime.now().minusMinutes(5))
                .build();
    }

    // ── processPayment ───────────────────────────────────────────────────────

    @Test
    void processPayment_cashMode_remainsPending_andDoesNotPublishEvent() {
        ProcessPaymentRequest req = ProcessPaymentRequest.builder()
                .bookingId(10L).userId(5L).amount(300.0)
                .mode(Payment.PaymentMode.CASH).build();

        given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.processPayment(req);

        assertThat(result.getStatus()).isEqualTo(Payment.PaymentStatus.PENDING);
        assertThat(result.getPaidAt()).isNull();
        then(eventPublisher).should(never()).publishPaymentCompleted(any());
    }

    @Test
    void processPayment_upiMode_setsPaid_setsTimestamp_andPublishesEvent() {
        ProcessPaymentRequest req = ProcessPaymentRequest.builder()
                .bookingId(10L).userId(5L).amount(300.0)
                .mode(Payment.PaymentMode.UPI).build();

        given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.processPayment(req);

        assertThat(result.getStatus()).isEqualTo(Payment.PaymentStatus.PAID);
        assertThat(result.getPaidAt()).isNotNull();
        assertThat(result.getTransactionId()).startsWith("TXN-");
        then(eventPublisher).should().publishPaymentCompleted(result);
    }

    @Test
    void processPayment_cardMode_setsPaid_andGeneratesTransactionId() {
        ProcessPaymentRequest req = ProcessPaymentRequest.builder()
                .bookingId(11L).userId(6L).amount(150.0)
                .mode(Payment.PaymentMode.CARD).build();

        given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.processPayment(req);

        assertThat(result.getStatus()).isEqualTo(Payment.PaymentStatus.PAID);
        assertThat(result.getTransactionId()).isNotBlank();
    }

    @Test
    void processPayment_usesProvidedTransactionId_whenExplicitlyGiven() {
        ProcessPaymentRequest req = ProcessPaymentRequest.builder()
                .bookingId(12L).userId(7L).amount(200.0)
                .mode(Payment.PaymentMode.CARD).transactionId("CUSTOM-TXN-001").build();

        given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.processPayment(req);

        assertThat(result.getTransactionId()).isEqualTo("CUSTOM-TXN-001");
    }

    @Test
    void processPayment_walletMode_setsPaidStatus() {
        ProcessPaymentRequest req = ProcessPaymentRequest.builder()
                .bookingId(13L).userId(8L).amount(100.0)
                .mode(Payment.PaymentMode.WALLET).build();

        given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.processPayment(req);

        assertThat(result.getStatus()).isEqualTo(Payment.PaymentStatus.PAID);
    }

    // ── updateStatus ─────────────────────────────────────────────────────────

    @Test
    void updateStatus_setsPaidAt_whenTransitioningToPaid() {
        Payment pending = Payment.builder()
                .paymentId(2L).status(Payment.PaymentStatus.PENDING).build();

        given(paymentRepository.findById(2L)).willReturn(Optional.of(pending));
        given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.updateStatus(2L, Payment.PaymentStatus.PAID);

        assertThat(result.getStatus()).isEqualTo(Payment.PaymentStatus.PAID);
        assertThat(result.getPaidAt()).isNotNull();
    }

    @Test
    void updateStatus_doesNotSetPaidAt_forFailedStatus() {
        given(paymentRepository.findById(1L)).willReturn(Optional.of(paidPayment));
        given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.updateStatus(1L, Payment.PaymentStatus.FAILED);

        assertThat(result.getStatus()).isEqualTo(Payment.PaymentStatus.FAILED);
    }

    // ── refundPayment ────────────────────────────────────────────────────────

    @Test
    void refundPayment_success_setsRefundedStatus_andTimestamp() {
        given(paymentRepository.findById(1L)).willReturn(Optional.of(paidPayment));
        given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.refundPayment(1L);

        assertThat(result.getStatus()).isEqualTo(Payment.PaymentStatus.REFUNDED);
        assertThat(result.getRefundedAt()).isNotNull();
        then(eventPublisher).should().publishPaymentRefunded(result);
    }

    @Test
    void refundPayment_throwsIllegalState_whenPaymentIsPending() {
        Payment pending = Payment.builder()
                .paymentId(3L).status(Payment.PaymentStatus.PENDING).build();

        given(paymentRepository.findById(3L)).willReturn(Optional.of(pending));

        assertThatThrownBy(() -> paymentService.refundPayment(3L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only PAID payments can be refunded");

        then(eventPublisher).should(never()).publishPaymentRefunded(any());
    }

    @Test
    void refundPayment_throwsIllegalState_whenPaymentAlreadyRefunded() {
        Payment refunded = Payment.builder()
                .paymentId(4L).status(Payment.PaymentStatus.REFUNDED).build();

        given(paymentRepository.findById(4L)).willReturn(Optional.of(refunded));

        assertThatThrownBy(() -> paymentService.refundPayment(4L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only PAID payments can be refunded");
    }

    // ── getById ──────────────────────────────────────────────────────────────

    @Test
    void getById_returnsPayment_whenFound() {
        given(paymentRepository.findById(1L)).willReturn(Optional.of(paidPayment));

        Payment result = paymentService.getById(1L);

        assertThat(result.getPaymentId()).isEqualTo(1L);
    }

    @Test
    void getById_throwsRuntimeException_whenNotFound() {
        given(paymentRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Payment not found: 999");
    }

    // ── getByBooking ─────────────────────────────────────────────────────────

    @Test
    void getByBooking_returnsPayment_whenFound() {
        given(paymentRepository.findByBookingId(10L)).willReturn(Optional.of(paidPayment));

        Payment result = paymentService.getByBooking(10L);

        assertThat(result.getBookingId()).isEqualTo(10L);
    }

    @Test
    void getByBooking_throwsRuntimeException_whenNoPaymentForBooking() {
        given(paymentRepository.findByBookingId(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getByBooking(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No payment found for booking: 99");
    }

    // ── getByUser ────────────────────────────────────────────────────────────

    @Test
    void getByUser_returnsPaymentsFromRepository() {
        given(paymentRepository.findByUserIdOrderByCreatedAtDesc(5L))
                .willReturn(List.of(paidPayment));

        List<Payment> result = paymentService.getByUser(5L);

        assertThat(result).hasSize(1).first().extracting(Payment::getUserId).isEqualTo(5L);
    }

    // ── getTotalRevenue ──────────────────────────────────────────────────────

    @Test
    void getTotalRevenue_delegatesToRepository() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();
        given(paymentRepository.sumRevenueBetween(from, to)).willReturn(15000.0);

        assertThat(paymentService.getTotalRevenue(from, to)).isEqualTo(15000.0);
    }

    // ── getPaymentStatus ─────────────────────────────────────────────────────

    @Test
    void getPaymentStatus_returnsStatusNameAsString() {
        given(paymentRepository.findById(1L)).willReturn(Optional.of(paidPayment));

        assertThat(paymentService.getPaymentStatus(1L)).isEqualTo("PAID");
    }
}
