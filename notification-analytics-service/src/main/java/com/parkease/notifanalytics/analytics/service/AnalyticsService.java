package com.parkease.notifanalytics.analytics.service;

import com.parkease.notifanalytics.analytics.entity.OccupancyLog;
import com.parkease.notifanalytics.analytics.repository.AnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepo;

    @RabbitListener(queues = "parkease.analytics.queue")
    public void handleBookingEvent(Map<String, Object> payload) {
        try {
            String status = (String) payload.get("status");
            if (!"ACTIVE".equals(status) && !"COMPLETED".equals(status) && !"CANCELLED".equals(status)) return;

            Long lotId = Long.valueOf(payload.get("lotId").toString());
            Long spotId = Long.valueOf(payload.get("spotId").toString());
            String vehicleType = (String) payload.getOrDefault("vehicleType", "UNKNOWN");

            OccupancyLog log = OccupancyLog.builder()
                    .lotId(lotId)
                    .spotId(spotId)
                    .timestamp(LocalDateTime.now())
                    .occupancyRate(0.0)
                    .availableSpots(0)
                    .totalSpots(0)
                    .vehicleType(vehicleType)
                    .build();

            analyticsRepo.save(log);
        } catch (Exception e) {
            log.error("Failed to log analytics event: {}", e.getMessage());
        }
    }

    public Double getOccupancyRate(Long lotId) {
        Double avg = analyticsRepo.avgOccupancyByLotId(lotId);
        return avg != null ? avg : 0.0;
    }

    public Map<Integer, Double> getOccupancyByHour(Long lotId) {
        List<Object[]> rows = analyticsRepo.getHourlyOccupancy(lotId);
        Map<Integer, Double> result = new HashMap<>();
        rows.forEach(r -> result.put(((Number) r[0]).intValue(), ((Number) r[1]).doubleValue()));
        return result;
    }

    public List<Integer> getPeakHours(Long lotId) {
        List<Object[]> rows = analyticsRepo.getPeakHours(lotId);
        return rows.stream().limit(5).map(r -> ((Number) r[0]).intValue()).toList();
    }

    public Map<String, Long> getSpotTypeUtilisation(Long lotId) {
        List<Object[]> rows = analyticsRepo.getSpotTypeUtilisation(lotId);
        Map<String, Long> result = new HashMap<>();
        rows.forEach(r -> result.put((String) r[0], ((Number) r[1]).longValue()));
        return result;
    }

    public List<OccupancyLog> getLotLogs(Long lotId, LocalDateTime from, LocalDateTime to) {
        return analyticsRepo.findByLotIdAndTimestampBetween(lotId, from, to);
    }

    public Map<String, Object> getPlatformSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalLogs", analyticsRepo.count());
        summary.put("generatedAt", LocalDateTime.now().toString());
        return summary;
    }
}
