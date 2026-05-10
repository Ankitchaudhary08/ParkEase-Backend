package com.parkease.notifanalytics.analytics.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "occupancy_logs", indexes = {
    @Index(name = "idx_lot_time", columnList = "lotId, timestamp"),
    @Index(name = "idx_vehicle_type", columnList = "vehicleType")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OccupancyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    @Column(nullable = false)
    private Long lotId;

    private Long spotId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private Double occupancyRate;

    @Column(nullable = false)
    private Integer availableSpots;

    @Column(nullable = false)
    private Integer totalSpots;

    @Column(length = 20)
    private String vehicleType;

    @Column(length = 20)
    private String spotType;
}
