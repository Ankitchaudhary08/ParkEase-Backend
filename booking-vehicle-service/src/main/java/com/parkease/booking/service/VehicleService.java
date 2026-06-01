package com.parkease.booking.service;

import com.parkease.booking.dto.RegisterVehicleRequest;
import com.parkease.booking.entity.Vehicle;
import com.parkease.booking.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    @Transactional
    public Vehicle registerVehicle(RegisterVehicleRequest request, Long ownerId) {
        if (vehicleRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw new IllegalArgumentException("License plate already registered");
        }
        Vehicle vehicle = Vehicle.builder()
                .ownerId(ownerId)
                .licensePlate(request.getLicensePlate().toUpperCase())
                .make(request.getMake())
                .model(request.getModel())
                .color(request.getColor())
                .vehicleType(request.getVehicleType())
                .isEV(request.isEV())
                .isActive(true)
                .build();
        return vehicleRepository.save(vehicle);
    }

    public List<Vehicle> getVehiclesByOwner(Long ownerId) {
        return vehicleRepository.findByOwnerIdAndIsActive(ownerId, true);
    }

    public Vehicle getVehicleById(Long vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
    }

    @Transactional
    public Vehicle updateVehicle(Long vehicleId, RegisterVehicleRequest request) {
        Vehicle vehicle = getVehicleById(vehicleId);
        if (request.getMake() != null) vehicle.setMake(request.getMake());
        if (request.getModel() != null) vehicle.setModel(request.getModel());
        if (request.getColor() != null) vehicle.setColor(request.getColor());
        vehicle.setEV(request.isEV());
        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public void deleteVehicle(Long vehicleId) {
        Vehicle vehicle = getVehicleById(vehicleId);
        vehicle.setActive(false);
        vehicleRepository.save(vehicle);
    }
}
