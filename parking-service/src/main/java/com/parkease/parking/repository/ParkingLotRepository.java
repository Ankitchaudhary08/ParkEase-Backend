package com.parkease.parking.repository;

import com.parkease.parking.entity.ParkingLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ParkingLotRepository extends JpaRepository<ParkingLot, Long> {
    List<ParkingLot> findByCityIgnoreCaseAndApprovalStatus(String city, ParkingLot.ApprovalStatus status);
    List<ParkingLot> findByManagerId(Long managerId);
    List<ParkingLot> findByIsOpenAndApprovalStatus(boolean isOpen, ParkingLot.ApprovalStatus status);
    List<ParkingLot> findByApprovalStatus(ParkingLot.ApprovalStatus status);

    @Query(value = """
        SELECT *, (6371 * acos(
            cos(radians(:lat)) * cos(radians(latitude)) *
            cos(radians(longitude) - radians(:lon)) +
            sin(radians(:lat)) * sin(radians(latitude))
        )) AS distance
        FROM parking_lots
        WHERE approval_status = 'APPROVED' AND is_open = true
        HAVING distance < :radiusKm
        ORDER BY distance
        """, nativeQuery = true)
    List<ParkingLot> findNearby(@Param("lat") double lat, @Param("lon") double lon, @Param("radiusKm") double radiusKm);

    List<ParkingLot> findByCityIgnoreCaseContainingAndApprovalStatus(String keyword, ParkingLot.ApprovalStatus status);
}
