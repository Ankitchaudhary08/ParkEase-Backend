package com.parkease.notifanalytics.analytics.resource;

import com.parkease.notifanalytics.analytics.entity.OccupancyLog;
import com.parkease.notifanalytics.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Occupancy, peak hours, and revenue analytics")
public class AnalyticsResource {

    private final AnalyticsService analyticsService;

    @GetMapping("/lots/{lotId}/occupancy-rate")
    @Operation(summary = "Get average occupancy rate for a lot")
    public ResponseEntity<Double> getOccupancyRate(@PathVariable Long lotId) {
        return ResponseEntity.ok(analyticsService.getOccupancyRate(lotId));
    }

    @GetMapping("/lots/{lotId}/occupancy-by-hour")
    @Operation(summary = "Get hourly occupancy breakdown")
    public ResponseEntity<Map<Integer, Double>> getOccupancyByHour(@PathVariable Long lotId) {
        return ResponseEntity.ok(analyticsService.getOccupancyByHour(lotId));
    }

    @GetMapping("/lots/{lotId}/peak-hours")
    @Operation(summary = "Get top 5 busiest hours for a lot")
    public ResponseEntity<List<Integer>> getPeakHours(@PathVariable Long lotId) {
        return ResponseEntity.ok(analyticsService.getPeakHours(lotId));
    }

    @GetMapping("/lots/{lotId}/spot-utilisation")
    @Operation(summary = "Get spot type utilisation breakdown")
    public ResponseEntity<Map<String, Long>> getSpotUtilisation(@PathVariable Long lotId) {
        return ResponseEntity.ok(analyticsService.getSpotTypeUtilisation(lotId));
    }

    @GetMapping("/lots/{lotId}/logs")
    @Operation(summary = "Get occupancy logs within date range")
    public ResponseEntity<List<OccupancyLog>> getLogs(
            @PathVariable Long lotId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(analyticsService.getLotLogs(lotId, from, to));
    }

    @GetMapping("/platform/summary")
    @Operation(summary = "Get platform-wide analytics summary (Admin)")
    public ResponseEntity<Map<String, Object>> getPlatformSummary(@RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(analyticsService.getPlatformSummary());
    }
}
