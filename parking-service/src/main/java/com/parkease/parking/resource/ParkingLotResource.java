package com.parkease.parking.resource;

import com.parkease.parking.dto.*;
import com.parkease.parking.entity.ParkingLot;
import com.parkease.parking.service.ParkingLotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/lots")
@RequiredArgsConstructor
@Tag(name = "Parking Lots", description = "Parking lot management and discovery")
public class ParkingLotResource {

    private final ParkingLotService lotService;

    @PostMapping
    @Operation(summary = "Register a new parking lot (Lot Manager)")
    public ResponseEntity<ParkingLot> createLot(
            @Valid @RequestBody CreateLotRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long managerId) {
        if (managerId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(lotService.createLot(request, managerId));
    }

    @GetMapping("/{lotId}")
    @Operation(summary = "Get lot by ID")
    public ResponseEntity<ParkingLot> getLotById(@PathVariable Long lotId) {
        return ResponseEntity.ok(lotService.getLotById(lotId));
    }

    @GetMapping("/city/{city}")
    @Operation(summary = "Get approved lots by city (public)")
    public ResponseEntity<List<ParkingLot>> getLotsByCity(@PathVariable String city) {
        return ResponseEntity.ok(lotService.getLotsByCity(city));
    }

    @GetMapping("/nearby")
    @Operation(summary = "Get nearby lots using Haversine formula (public)")
    public ResponseEntity<List<ParkingLot>> getNearby(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "5.0") double radius) {
        return ResponseEntity.ok(lotService.getNearbyLots(lat, lon, radius));
    }

    @GetMapping("/manager")
    @Operation(summary = "Get lots managed by authenticated manager")
    public ResponseEntity<List<ParkingLot>> getLotsByManager(
            @RequestHeader(value = "X-User-Id", required = false) Long managerId) {
        if (managerId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(lotService.getLotsByManager(managerId));
    }

    @PutMapping("/{lotId}")
    @Operation(summary = "Update lot details")
    public ResponseEntity<ParkingLot> updateLot(
            @PathVariable Long lotId,
            @RequestBody UpdateLotRequest request) {
        return ResponseEntity.ok(lotService.updateLot(lotId, request));
    }

    @PutMapping("/{lotId}/toggle-open")
    @Operation(summary = "Toggle lot open/closed status")
    public ResponseEntity<ParkingLot> toggleOpen(@PathVariable Long lotId) {
        return ResponseEntity.ok(lotService.toggleOpen(lotId));
    }

    @PutMapping("/{lotId}/approve")
    @Operation(summary = "Approve or reject a lot (Admin only)")
    public ResponseEntity<ParkingLot> approveLot(
            @PathVariable Long lotId,
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (role == null) return ResponseEntity.status(401).build();
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        boolean approved = Boolean.TRUE.equals(body.get("approved"));
        String reason = (String) body.getOrDefault("reason", null);
        return ResponseEntity.ok(lotService.approveLot(lotId, approved, reason));
    }

    @GetMapping("/pending")
    @Operation(summary = "Get lots pending approval (Admin)")
    public ResponseEntity<List<ParkingLot>> getPending(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (role == null) return ResponseEntity.status(401).build();
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(lotService.getPendingApproval());
    }

    @GetMapping("/all")
    @Operation(summary = "Get all lots — Admin only")
    public ResponseEntity<List<ParkingLot>> getAllLots(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (role == null) return ResponseEntity.status(401).build();
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(lotService.getAllLots());
    }

    @PutMapping("/{lotId}/decrement")
    @Operation(summary = "Decrement available spots (called by booking-service)")
    public ResponseEntity<Void> decrementAvailable(@PathVariable Long lotId) {
        lotService.decrementAvailable(lotId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{lotId}/increment")
    @Operation(summary = "Increment available spots (called by booking-service)")
    public ResponseEntity<Void> incrementAvailable(@PathVariable Long lotId) {
        lotService.incrementAvailable(lotId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{lotId}")
    @Operation(summary = "Delete a lot")
    public ResponseEntity<Void> deleteLot(@PathVariable Long lotId) {
        lotService.deleteLot(lotId);
        return ResponseEntity.noContent().build();
    }
}
