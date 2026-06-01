package com.parkease.parking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "parking_lots", indexes = {
    @Index(name = "idx_city", columnList = "city"),
    @Index(name = "idx_manager", columnList = "managerId"),
    @Index(name = "idx_location", columnList = "latitude, longitude")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ParkingLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lotId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 300)
    private String address;

    @Column(nullable = false, length = 100)
    private String city;

    private Double latitude;

    private Double longitude;

    @Column(nullable = false)
    private Integer totalSpots;

    @Column(nullable = false)
    @Builder.Default
    private Integer availableSpots = 0;

    @Column(nullable = false)
    private Long managerId;

    @Column(nullable = false)
    @Builder.Default
    private boolean isOpen = true;

    private LocalTime openTime;
    private LocalTime closeTime;

    @Column(length = 512)
    private String imageUrl;

    private Double hourlyRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Column(length = 500)
    private String rejectionReason;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "lot", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ParkingSpot> spots;

    public enum ApprovalStatus { PENDING, APPROVED, REJECTED }
}
