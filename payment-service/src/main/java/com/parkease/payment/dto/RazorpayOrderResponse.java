package com.parkease.payment.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RazorpayOrderResponse {

    private String razorpayOrderId;
    private Long   amountInPaise;
    private String currency;
    private String keyId;
    private Long   bookingId;
    private Long   userId;
    private String description;
}
