package com.parkease.parking.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateLotRequest {
    @NotBlank private String name;
    @NotBlank private String address;
    @NotBlank private String city;
    @DecimalMin("-90.0") @DecimalMax("90.0") private Double latitude;
    @DecimalMin("-180.0") @DecimalMax("180.0") private Double longitude;
    @NotNull @Min(1) private Integer totalSpots;
    private Double hourlyRate;
    private LocalTime openTime;
    private LocalTime closeTime;
    private String imageUrl;
}
