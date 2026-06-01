package com.parkease.parking.resource;

import com.parkease.parking.dto.AddSpotRequest;
import com.parkease.parking.entity.ParkingSpot;
import com.parkease.parking.service.ParkingSpotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/spots")
@RequiredArgsConstructor
@Tag(name = "Parking Spots", description = "Spot management within parking lots")
public class ParkingSpotResource {

    private final ParkingSpotService spotService;

    @PostMapping("/lot/{lotId}")
    @Operation(summary = "Add a single spot to a lot")
    public ResponseEntity<ParkingSpot> addSpot(
            @PathVariable Long lotId,
            @Valid @RequestBody AddSpotRequest request) {
        return ResponseEntity.ok(spotService.addSpot(lotId, request));
    }

    @PostMapping("/lot/{lotId}/bulk")
    @Operation(summary = "Bulk add spots to a lot")
    public ResponseEntity<List<ParkingSpot>> addBulkSpots(
            @PathVariable Long lotId,
            @RequestBody List<AddSpotRequest> requests) {
        return ResponseEntity.ok(spotService.addBulkSpots(lotId, requests));
    }

    @GetMapping("/lot/{lotId}")
    @Operation(summary = "Get all spots for a lot")
    public ResponseEntity<List<ParkingSpot>> getSpotsByLot(@PathVariable Long lotId) {
        return ResponseEntity.ok(spotService.getSpotsByLot(lotId));
    }

    @GetMapping("/lot/{lotId}/available")
    @Operation(summary = "Get available spots for a lot (Redis-cached)")
    public ResponseEntity<List<ParkingSpot>> getAvailable(@PathVariable Long lotId) {
        return ResponseEntity.ok(spotService.getAvailableSpots(lotId));
    }

    @GetMapping("/lot/{lotId}/available/count")
    @Operation(summary = "Get available spot count (Redis-cached)")
    public ResponseEntity<Integer> countAvailable(@PathVariable Long lotId) {
        return ResponseEntity.ok(spotService.countAvailable(lotId));
    }

    @PutMapping("/{spotId}/occupy")
    @Operation(summary = "Mark spot as Occupied (called by booking-service)")
    public ResponseEntity<ParkingSpot> occupySpot(@PathVariable Long spotId) {
        return ResponseEntity.ok(spotService.occupySpot(spotId));
    }

    @PutMapping("/{spotId}/reserve")
    @Operation(summary = "Mark spot as Reserved")
    public ResponseEntity<ParkingSpot> reserveSpot(@PathVariable Long spotId) {
        return ResponseEntity.ok(spotService.reserveSpot(spotId));
    }

    @PutMapping("/{spotId}/release")
    @Operation(summary = "Release spot back to Available")
    public ResponseEntity<ParkingSpot> releaseSpot(@PathVariable Long spotId) {
        return ResponseEntity.ok(spotService.releaseSpot(spotId));
    }

    @PutMapping("/{spotId}")
    @Operation(summary = "Update spot details")
    public ResponseEntity<ParkingSpot> updateSpot(
            @PathVariable Long spotId,
            @RequestBody AddSpotRequest request) {
        return ResponseEntity.ok(spotService.updateSpot(spotId, request));
    }

    @DeleteMapping("/{spotId}")
    @Operation(summary = "Delete a spot")
    public ResponseEntity<Void> deleteSpot(@PathVariable Long spotId) {
        spotService.deleteSpot(spotId);
        return ResponseEntity.noContent().build();
    }
}
