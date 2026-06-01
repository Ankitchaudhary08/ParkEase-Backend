package com.parkease.booking.dto;

import com.parkease.booking.entity.Booking;
import com.parkease.booking.entity.Vehicle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateBookingRequest {
    @NotNull private Long lotId;
    @NotNull private Long spotId;
    @NotBlank private String vehiclePlate;
    @NotNull private Booking.BookingType bookingType;
    @NotNull private LocalDateTime startTime;
    @NotNull private LocalDateTime endTime;
    @NotNull private Double pricePerHour;
    private Vehicle.VehicleType vehicleType;
}
