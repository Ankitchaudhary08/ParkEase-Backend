package com.parkease.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RazorpayOrderRequest {

    @NotNull
    private Long bookingId;

    @NotNull
    private Long userId;

    @NotNull @Positive
    private Double amount;

    @Builder.Default
    private String currency = "INR";

    private String description;
}
