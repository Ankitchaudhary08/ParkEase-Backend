package com.parkease.payment.service;

import com.parkease.payment.dto.RazorpayVerifyRequest;
import com.parkease.payment.entity.Payment;
import com.parkease.payment.messaging.PaymentEventPublisher;
import com.parkease.payment.repository.PaymentRepository;
import com.razorpay.RazorpayClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class RazorpayServiceTest {

    @Mock RazorpayClient razorpayClient;
    @Mock PaymentRepository paymentRepository;
    @Mock PaymentEventPublisher eventPublisher;

    @InjectMocks RazorpayService razorpayService;

    static final String TEST_KEY_ID     = "rzp_test_key";
    static final String TEST_KEY_SECRET = "test_secret_key_for_unit_tests_32c";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(razorpayService, "razorpayKeyId",     TEST_KEY_ID);
        ReflectionTestUtils.setField(razorpayService, "razorpayKeySecret", TEST_KEY_SECRET);
    }

    // ── verifyAndCapture ─────────────────────────────────────────────────────

    @Test
    void verifyAndCapture_success_withValidSignature() throws Exception {
        String orderId    = "order_ABC123";
        String paymentId  = "pay_XYZ789";
        String validSig   = hmac(orderId + "|" + paymentId, TEST_KEY_SECRET);

        RazorpayVerifyRequest req = RazorpayVerifyRequest.builder()
                .bookingId(1L).userId(2L).amount(300.0)
                .razorpayOrderId(orderId)
                .razorpayPaymentId(paymentId)
                .razorpaySignature(validSig)
                .build();

        given(paymentRepository.findByBookingId(1L)).willReturn(Optional.empty());
        given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setPaymentId(10L);
            return p;
        });

        Payment result = razorpayService.verifyAndCapture(req);

        assertThat(result.getStatus()).isEqualTo(Payment.PaymentStatus.PAID);
        assertThat(result.getRazorpayOrderId()).isEqualTo(orderId);
        assertThat(result.getRazorpayPaymentId()).isEqualTo(paymentId);
        assertThat(result.getPaidAt()).isNotNull();
        then(eventPublisher).should().publishPaymentCompleted(result);
    }

    @Test
    void verifyAndCapture_updatesExistingPaymentRecord_whenOneAlreadyExists() throws Exception {
        String orderId   = "order_EXIST01";
        String paymentId = "pay_EXIST01";
        String validSig  = hmac(orderId + "|" + paymentId, TEST_KEY_SECRET);

        Payment existingPayment = Payment.builder()
                .paymentId(5L).bookingId(1L).userId(2L).amount(300.0)
                .status(Payment.PaymentStatus.PENDING)
                .mode(Payment.PaymentMode.UPI)
                .build();

        RazorpayVerifyRequest req = RazorpayVerifyRequest.builder()
                .bookingId(1L).userId(2L).amount(300.0)
                .razorpayOrderId(orderId)
                .razorpayPaymentId(paymentId)
                .razorpaySignature(validSig)
                .build();

        given(paymentRepository.findByBookingId(1L)).willReturn(Optional.of(existingPayment));
        given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));

        Payment result = razorpayService.verifyAndCapture(req);

        assertThat(result.getPaymentId()).isEqualTo(5L); // same record
        assertThat(result.getStatus()).isEqualTo(Payment.PaymentStatus.PAID);
    }

    @Test
    void verifyAndCapture_throwsIllegalArgument_whenSignatureIsInvalid() {
        RazorpayVerifyRequest req = RazorpayVerifyRequest.builder()
                .bookingId(1L).userId(2L).amount(300.0)
                .razorpayOrderId("order_ABC123")
                .razorpayPaymentId("pay_XYZ789")
                .razorpaySignature("completely-wrong-signature")
                .build();

        assertThatThrownBy(() -> razorpayService.verifyAndCapture(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payment signature verification failed");

        then(paymentRepository).should(never()).save(any());
        then(eventPublisher).should(never()).publishPaymentCompleted(any());
    }

    @Test
    void verifyAndCapture_throwsIllegalArgument_whenSignatureUsesWrongOrderId() throws Exception {
        String orderId   = "order_CORRECT";
        String paymentId = "pay_XYZ789";
        // Signature computed with a different orderId — should fail
        String wrongSig  = hmac("order_WRONG|" + paymentId, TEST_KEY_SECRET);

        RazorpayVerifyRequest req = RazorpayVerifyRequest.builder()
                .bookingId(1L).userId(2L).amount(300.0)
                .razorpayOrderId(orderId)
                .razorpayPaymentId(paymentId)
                .razorpaySignature(wrongSig)
                .build();

        assertThatThrownBy(() -> razorpayService.verifyAndCapture(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payment signature verification failed");
    }

    @Test
    void verifyAndCapture_setsTransactionIdToRazorpayPaymentId() throws Exception {
        String orderId   = "order_TXNTEST";
        String paymentId = "pay_TXNTEST1";
        String sig       = hmac(orderId + "|" + paymentId, TEST_KEY_SECRET);

        RazorpayVerifyRequest req = RazorpayVerifyRequest.builder()
                .bookingId(2L).userId(3L).amount(500.0)
                .razorpayOrderId(orderId)
                .razorpayPaymentId(paymentId)
                .razorpaySignature(sig)
                .build();

        given(paymentRepository.findByBookingId(2L)).willReturn(Optional.empty());
        given(paymentRepository.save(any(Payment.class))).willAnswer(inv -> inv.getArgument(0));

        Payment result = razorpayService.verifyAndCapture(req);

        assertThat(result.getTransactionId()).isEqualTo(paymentId);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static String hmac(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
}
