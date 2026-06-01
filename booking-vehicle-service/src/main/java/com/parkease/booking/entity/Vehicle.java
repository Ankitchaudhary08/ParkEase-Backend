package com.parkease.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;

@Entity
@Table(name = "vehicles", indexes = {
    @Index(name = "idx_owner", columnList = "ownerId"),
    @Index(name = "idx_plate", columnList = "licensePlate")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long vehicleId;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false, unique = true, length = 20)
    private String licensePlate;

    @Column(length = 100)
    private String make;

    @Column(length = 100)
    private String model;

    @Column(length = 50)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VehicleType vehicleType;

    @Column(nullable = false)
    @Builder.Default
    private boolean isEV = false;

    @CreationTimestamp
    private LocalDate registeredAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;

    public enum VehicleType { TWO_WHEELER, FOUR_WHEELER, HEAVY }
}
