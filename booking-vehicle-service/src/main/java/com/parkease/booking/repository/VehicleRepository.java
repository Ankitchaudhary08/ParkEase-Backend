package com.parkease.booking.repository;

import com.parkease.booking.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByOwnerId(Long ownerId);
    Optional<Vehicle> findByLicensePlate(String licensePlate);
    List<Vehicle> findByVehicleType(Vehicle.VehicleType vehicleType);
    List<Vehicle> findByIsEV(boolean isEV);
    boolean existsByLicensePlate(String licensePlate);
    List<Vehicle> findByOwnerIdAndIsActive(Long ownerId, boolean isActive);
}
