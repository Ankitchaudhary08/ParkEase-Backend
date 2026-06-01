package com.parkease.payment.dto;

import com.parkease.payment.entity.Payment;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProcessPaymentRequest {
    @NotNull private Long bookingId;
    @NotNull private Long userId;
    @NotNull @Positive private Double amount;
    @NotNull private Payment.PaymentMode mode;
    private String description;
    private String transactionId;
}
