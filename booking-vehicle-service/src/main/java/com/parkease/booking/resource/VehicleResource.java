package com.parkease.booking.resource;

import com.parkease.booking.dto.RegisterVehicleRequest;
import com.parkease.booking.entity.Vehicle;
import com.parkease.booking.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@Tag(name = "Vehicles", description = "Driver vehicle registration and management")
public class VehicleResource {

    private final VehicleService vehicleService;

    @PostMapping
    @Operation(summary = "Register a vehicle")
    public ResponseEntity<Vehicle> register(
            @Valid @RequestBody RegisterVehicleRequest request,
            @RequestHeader("X-User-Id") Long ownerId) {
        return ResponseEntity.ok(vehicleService.registerVehicle(request, ownerId));
    }

    @GetMapping
    @Operation(summary = "Get all vehicles for authenticated driver")
    public ResponseEntity<List<Vehicle>> getMyVehicles(@RequestHeader("X-User-Id") Long ownerId) {
        return ResponseEntity.ok(vehicleService.getVehiclesByOwner(ownerId));
    }

    @GetMapping("/{vehicleId}")
    public ResponseEntity<Vehicle> getById(@PathVariable Long vehicleId) {
        return ResponseEntity.ok(vehicleService.getVehicleById(vehicleId));
    }

    @PutMapping("/{vehicleId}")
    public ResponseEntity<Vehicle> update(
            @PathVariable Long vehicleId,
            @RequestBody RegisterVehicleRequest request) {
        return ResponseEntity.ok(vehicleService.updateVehicle(vehicleId, request));
    }

    @DeleteMapping("/{vehicleId}")
    public ResponseEntity<Void> delete(@PathVariable Long vehicleId) {
        vehicleService.deleteVehicle(vehicleId);
        return ResponseEntity.noContent().build();
    }
}
