package com.parkease.parking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "parking_spots", indexes = {
    @Index(name = "idx_lot_status", columnList = "lot_id, status"),
    @Index(name = "idx_lot_type", columnList = "lot_id, spotType"),
    @Index(name = "idx_lot_vehicle", columnList = "lot_id, vehicleType")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ParkingSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long spotId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id", nullable = false)
    private ParkingLot lot;

    @Column(nullable = false, length = 20)
    private String spotNumber;

    @Column(nullable = false)
    private Integer floor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SpotType spotType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VehicleType vehicleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SpotStatus status = SpotStatus.AVAILABLE;

    @Column(nullable = false)
    @Builder.Default
    private boolean isHandicapped = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean isEVCharging = false;

    @Column(nullable = false)
    private Double pricePerHour;

    @Version
    private Long version;

    public enum SpotType { COMPACT, STANDARD, LARGE, MOTORBIKE, EV_ONLY }
    public enum VehicleType { TWO_WHEELER, FOUR_WHEELER, HEAVY }
    public enum SpotStatus { AVAILABLE, RESERVED, OCCUPIED }
}
