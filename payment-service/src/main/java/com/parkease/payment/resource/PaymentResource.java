package com.parkease.payment.resource;

import com.parkease.payment.dto.*;
import com.parkease.payment.entity.Payment;
import com.parkease.payment.service.PaymentService;
import com.parkease.payment.service.RazorpayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Payment processing, refunds, and revenue reporting")
public class PaymentResource {

    private final PaymentService paymentService;
    private final RazorpayService razorpayService;

    @Autowired
    public PaymentResource(PaymentService paymentService, RazorpayService razorpayService) {
        this.paymentService = paymentService;
        this.razorpayService = razorpayService;
    }

    // ─── Standard Payments ─────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Process a payment for a booking (CASH / WALLET / CARD)")
    public ResponseEntity<Payment> process(@Valid @RequestBody ProcessPaymentRequest request) {
        return ResponseEntity.ok(paymentService.processPayment(request));
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<Payment> getById(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.getById(paymentId));
    }

    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Get payment by booking ID")
    public ResponseEntity<Payment> getByBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(paymentService.getByBooking(bookingId));
    }

    @GetMapping("/user")
    @Operation(summary = "Get payment history for authenticated user")
    public ResponseEntity<List<Payment>> getByUser(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(paymentService.getByUser(userId));
    }

    @PostMapping("/{paymentId}/refund")
    @Operation(summary = "Refund a paid payment")
    public ResponseEntity<Payment> refund(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.refundPayment(paymentId));
    }

    @PutMapping("/{paymentId}/status")
    @Operation(summary = "Update payment status (e.g., confirm CASH payment at gate)")
    public ResponseEntity<Payment> updateStatus(
            @PathVariable Long paymentId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
                paymentService.updateStatus(paymentId, Payment.PaymentStatus.valueOf(body.get("status")))
        );
    }

    @GetMapping("/status/{paymentId}")
    @Operation(summary = "Get payment status string")
    public ResponseEntity<String> getStatus(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(paymentId));
    }

    @GetMapping("/revenue")
    @Operation(summary = "Get total revenue between dates (Admin / Manager)")
    public ResponseEntity<Double> getRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role) && !"MANAGER".equals(role)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(paymentService.getTotalRevenue(from, to));
    }

    // ─── Razorpay UPI / Card Checkout ──────────────────────────────────────────

    /**
     * Step 1 — Create a Razorpay order.
     * Angular opens the Razorpay checkout widget using the returned razorpayOrderId + keyId.
     *
     * POST /api/v1/payments/razorpay/create-order
     * Body: { bookingId, userId, amount, currency:"INR", description }
     * Response: { razorpayOrderId, amountInPaise, currency, keyId, bookingId, userId }
     */
    @PostMapping("/razorpay/create-order")
    @Operation(
        summary = "Create Razorpay order for UPI / card payment",
        description = "Returns a Razorpay order ID and key. " +
                      "Pass these to the Razorpay checkout.js widget on the frontend. " +
                      "Use test key rzp_test_* with test UPI ID success@razorpay."
    )
    public ResponseEntity<RazorpayOrderResponse> createRazorpayOrder(
            @Valid @RequestBody RazorpayOrderRequest request) {
        return ResponseEntity.ok(razorpayService.createOrder(request));
    }

    /**
     * Step 2 — Verify Razorpay payment signature after the user completes checkout.
     * Signature = HMAC-SHA256(razorpayOrderId + "|" + razorpayPaymentId, keySecret).
     * On success, persists a PAID Payment record and fires the payment.completed RabbitMQ event.
     *
     * POST /api/v1/payments/razorpay/verify
     * Body: { bookingId, userId, razorpayOrderId, razorpayPaymentId, razorpaySignature, amount }
     * Response: Payment entity with status=PAID
     */
    @PostMapping("/razorpay/verify")
    @Operation(
        summary = "Verify Razorpay signature and capture payment",
        description = "Validates HMAC-SHA256 signature received from Razorpay checkout callback. " +
                      "On success updates Payment status to PAID and publishes payment.completed event."
    )
    public ResponseEntity<Payment> verifyRazorpayPayment(
            @Valid @RequestBody RazorpayVerifyRequest request) {
        return ResponseEntity.ok(razorpayService.verifyAndCapture(request));
    }
}
