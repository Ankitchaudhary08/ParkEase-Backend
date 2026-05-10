package com.parkease.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings", indexes = {
    @Index(name = "idx_user", columnList = "userId"),
    @Index(name = "idx_lot", columnList = "lotId"),
    @Index(name = "idx_spot", columnList = "spotId"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_plate", columnList = "vehiclePlate")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookingId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long lotId;

    @Column(nullable = false)
    private Long spotId;

    @Column(nullable = false, length = 20)
    private String vehiclePlate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Vehicle.VehicleType vehicleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingType bookingType;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BookingStatus status = BookingStatus.RESERVED;

    private Double totalAmount;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Double pricePerHour;

    public enum BookingType { PRE_BOOKING, WALK_IN }
    public enum BookingStatus { RESERVED, ACTIVE, COMPLETED, CANCELLED }
}
