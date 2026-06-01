package com.parkease.parking.dto;

import lombok.*;
import java.time.LocalTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UpdateLotRequest {
    private String name;
    private String address;
    private LocalTime openTime;
    private LocalTime closeTime;
    private String imageUrl;
}
