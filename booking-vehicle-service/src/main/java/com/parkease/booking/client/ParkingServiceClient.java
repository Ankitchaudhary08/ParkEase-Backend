package com.parkease.booking.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "parking-service")
public interface ParkingServiceClient {

    @PutMapping("/api/v1/spots/{spotId}/reserve")
    void reserveSpot(@PathVariable Long spotId);

    @PutMapping("/api/v1/spots/{spotId}/occupy")
    void occupySpot(@PathVariable Long spotId);

    @PutMapping("/api/v1/spots/{spotId}/release")
    void releaseSpot(@PathVariable Long spotId);

    @PutMapping("/api/v1/lots/{lotId}/decrement")
    void decrementAvailable(@PathVariable Long lotId);

    @PutMapping("/api/v1/lots/{lotId}/increment")
    void incrementAvailable(@PathVariable Long lotId);
}
