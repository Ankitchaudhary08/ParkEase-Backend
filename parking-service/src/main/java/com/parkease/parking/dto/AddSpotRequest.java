package com.parkease.parking.dto;

import com.parkease.parking.entity.ParkingSpot;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AddSpotRequest {
    @NotBlank private String spotNumber;
    @NotNull private Integer floor;
    @NotNull private ParkingSpot.SpotType spotType;
    @NotNull private ParkingSpot.VehicleType vehicleType;
    private boolean isHandicapped;
    private boolean isEVCharging;
    @NotNull private Double pricePerHour;
}
