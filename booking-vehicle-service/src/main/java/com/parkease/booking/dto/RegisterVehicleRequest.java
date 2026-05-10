package com.parkease.booking.dto;

import com.parkease.booking.entity.Vehicle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegisterVehicleRequest {
    @NotBlank private String licensePlate;
    private String make;
    private String model;
    private String color;
    @NotNull private Vehicle.VehicleType vehicleType;
    private boolean isEV;
}
