package com.parkease.parking.repository;

import com.parkease.parking.entity.ParkingSpot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, Long> {
    List<ParkingSpot> findByLotLotId(Long lotId);
    List<ParkingSpot> findByLotLotIdAndStatus(Long lotId, ParkingSpot.SpotStatus status);
    List<ParkingSpot> findByLotLotIdAndSpotType(Long lotId, ParkingSpot.SpotType spotType);
    List<ParkingSpot> findByLotLotIdAndVehicleType(Long lotId, ParkingSpot.VehicleType vehicleType);
    List<ParkingSpot> findByLotLotIdAndIsEVCharging(Long lotId, boolean isEVCharging);
    int countByLotLotIdAndStatus(Long lotId, ParkingSpot.SpotStatus status);

    @Query("SELECT s FROM ParkingSpot s WHERE s.lot.lotId = :lotId AND s.status = 'AVAILABLE' AND s.vehicleType = :vehicleType")
    List<ParkingSpot> findAvailableByLotAndVehicleType(@Param("lotId") Long lotId, @Param("vehicleType") ParkingSpot.VehicleType vehicleType);
}
