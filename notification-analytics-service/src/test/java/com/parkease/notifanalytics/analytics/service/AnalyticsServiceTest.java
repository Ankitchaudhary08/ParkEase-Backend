package com.parkease.notifanalytics.analytics.service;

import com.parkease.notifanalytics.analytics.entity.OccupancyLog;
import com.parkease.notifanalytics.analytics.repository.AnalyticsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock AnalyticsRepository analyticsRepo;

    @InjectMocks AnalyticsService analyticsService;

    // ── getOccupancyRate ─────────────────────────────────────────────────────

    @Test
    void getOccupancyRate_returnsValueFromRepository() {
        given(analyticsRepo.avgOccupancyByLotId(1L)).willReturn(0.75);

        assertThat(analyticsService.getOccupancyRate(1L)).isEqualTo(0.75);
    }

    @Test
    void getOccupancyRate_returnsZero_whenRepositoryReturnsNull() {
        given(analyticsRepo.avgOccupancyByLotId(1L)).willReturn(null);

        assertThat(analyticsService.getOccupancyRate(1L)).isEqualTo(0.0);
    }

    @Test
    void getOccupancyRate_returnsFullOccupancy_whenAllSpotsOccupied() {
        given(analyticsRepo.avgOccupancyByLotId(2L)).willReturn(1.0);

        assertThat(analyticsService.getOccupancyRate(2L)).isEqualTo(1.0);
    }

    // ── getOccupancyByHour ───────────────────────────────────────────────────

    @Test
    void getOccupancyByHour_mapsRowsToHourDoubleMap() {
        List<Object[]> rows = List.of(
                new Object[]{9, 5.0},
                new Object[]{10, 8.0},
                new Object[]{17, 3.5}
        );
        given(analyticsRepo.getHourlyOccupancy(1L)).willReturn(rows);

        Map<Integer, Double> result = analyticsService.getOccupancyByHour(1L);

        assertThat(result)
                .containsEntry(9, 5.0)
                .containsEntry(10, 8.0)
                .containsEntry(17, 3.5)
                .hasSize(3);
    }

    @Test
    void getOccupancyByHour_returnsEmptyMap_whenNoData() {
        given(analyticsRepo.getHourlyOccupancy(1L)).willReturn(List.of());

        assertThat(analyticsService.getOccupancyByHour(1L)).isEmpty();
    }

    // ── getPeakHours ─────────────────────────────────────────────────────────

    @Test
    void getPeakHours_returnsAtMostFiveHours() {
        List<Object[]> rows = List.of(
                new Object[]{9}, new Object[]{10}, new Object[]{17},
                new Object[]{18}, new Object[]{12}, new Object[]{8} // 6 rows
        );
        given(analyticsRepo.getPeakHours(1L)).willReturn(rows);

        List<Integer> result = analyticsService.getPeakHours(1L);

        assertThat(result).hasSize(5).containsExactly(9, 10, 17, 18, 12);
    }

    @Test
    void getPeakHours_returnsLessThanFive_whenFewDataPoints() {
        List<Object[]> rows = List.of(new Object[]{9}, new Object[]{10});
        given(analyticsRepo.getPeakHours(1L)).willReturn(rows);

        List<Integer> result = analyticsService.getPeakHours(1L);

        assertThat(result).hasSize(2).containsExactly(9, 10);
    }

    @Test
    void getPeakHours_returnsEmpty_whenNoLogs() {
        given(analyticsRepo.getPeakHours(1L)).willReturn(List.of());

        assertThat(analyticsService.getPeakHours(1L)).isEmpty();
    }

    // ── getSpotTypeUtilisation ───────────────────────────────────────────────

    @Test
    void getSpotTypeUtilisation_mapsRowsToStringLongMap() {
        List<Object[]> rows = List.of(
                new Object[]{"COMPACT", 40L},
                new Object[]{"LARGE", 10L},
                new Object[]{"EV_ONLY", 5L}
        );
        given(analyticsRepo.getSpotTypeUtilisation(1L)).willReturn(rows);

        Map<String, Long> result = analyticsService.getSpotTypeUtilisation(1L);

        assertThat(result)
                .containsEntry("COMPACT", 40L)
                .containsEntry("LARGE", 10L)
                .containsEntry("EV_ONLY", 5L)
                .hasSize(3);
    }

    @Test
    void getSpotTypeUtilisation_returnsEmptyMap_whenNoData() {
        given(analyticsRepo.getSpotTypeUtilisation(1L)).willReturn(List.of());

        assertThat(analyticsService.getSpotTypeUtilisation(1L)).isEmpty();
    }

    // ── getLotLogs ───────────────────────────────────────────────────────────

    @Test
    void getLotLogs_returnsLogsForGivenTimeRange() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to   = LocalDateTime.now();
        OccupancyLog log1 = OccupancyLog.builder().logId(1L).lotId(1L).spotId(5L).build();
        OccupancyLog log2 = OccupancyLog.builder().logId(2L).lotId(1L).spotId(6L).build();

        given(analyticsRepo.findByLotIdAndTimestampBetween(1L, from, to))
                .willReturn(List.of(log1, log2));

        List<OccupancyLog> result = analyticsService.getLotLogs(1L, from, to);

        assertThat(result).hasSize(2)
                .allMatch(l -> l.getLotId().equals(1L));
    }

    @Test
    void getLotLogs_returnsEmptyList_whenNoLogsInRange() {
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to   = LocalDateTime.now();

        given(analyticsRepo.findByLotIdAndTimestampBetween(1L, from, to))
                .willReturn(List.of());

        assertThat(analyticsService.getLotLogs(1L, from, to)).isEmpty();
    }

    // ── getPlatformSummary ───────────────────────────────────────────────────

    @Test
    void getPlatformSummary_containsTotalLogsAndGeneratedAt() {
        given(analyticsRepo.count()).willReturn(500L);

        Map<String, Object> summary = analyticsService.getPlatformSummary();

        assertThat(summary).containsKey("totalLogs").containsKey("generatedAt");
        assertThat(summary.get("totalLogs")).isEqualTo(500L);
        assertThat(summary.get("generatedAt")).isInstanceOf(String.class);
    }

    @Test
    void getPlatformSummary_returnsZeroTotalLogs_whenNoData() {
        given(analyticsRepo.count()).willReturn(0L);

        Map<String, Object> summary = analyticsService.getPlatformSummary();

        assertThat(summary.get("totalLogs")).isEqualTo(0L);
    }

    // ── handleBookingEvent ───────────────────────────────────────────────────

    @Test
    void handleBookingEvent_ACTIVE_savesOccupancyLog() {
        Map<String, Object> payload = Map.of(
                "status", "ACTIVE", "lotId", 1L, "spotId", 5L, "vehicleType", "FOUR_WHEELER");

        analyticsService.handleBookingEvent(payload);

        then(analyticsRepo).should().save(argThat(log ->
                log.getLotId().equals(1L) &&
                log.getSpotId().equals(5L) &&
                "FOUR_WHEELER".equals(log.getVehicleType())));
    }

    @Test
    void handleBookingEvent_COMPLETED_savesOccupancyLog() {
        Map<String, Object> payload = Map.of(
                "status", "COMPLETED", "lotId", 2L, "spotId", 10L);

        analyticsService.handleBookingEvent(payload);

        then(analyticsRepo).should().save(any(OccupancyLog.class));
    }

    @Test
    void handleBookingEvent_CANCELLED_savesOccupancyLog() {
        Map<String, Object> payload = Map.of(
                "status", "CANCELLED", "lotId", 3L, "spotId", 7L);

        analyticsService.handleBookingEvent(payload);

        then(analyticsRepo).should().save(any(OccupancyLog.class));
    }

    @Test
    void handleBookingEvent_RESERVED_doesNotSaveLog() {
        Map<String, Object> payload = Map.of(
                "status", "RESERVED", "lotId", 1L, "spotId", 5L);

        analyticsService.handleBookingEvent(payload);

        then(analyticsRepo).should(never()).save(any());
    }

    @Test
    void handleBookingEvent_usesUNKNOWN_vehicleType_whenFieldMissing() {
        Map<String, Object> payload = Map.of(
                "status", "ACTIVE", "lotId", 1L, "spotId", 5L); // no vehicleType key

        analyticsService.handleBookingEvent(payload);

        then(analyticsRepo).should().save(
                argThat(log -> "UNKNOWN".equals(log.getVehicleType())));
    }

    @Test
    void handleBookingEvent_doesNotThrow_onMalformedPayload() {
        assertThatCode(() -> analyticsService.handleBookingEvent(Map.of()))
                .doesNotThrowAnyException();
    }

    @Test
    void handleBookingEvent_setsTimestampToNow() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        Map<String, Object> payload = Map.of(
                "status", "ACTIVE", "lotId", 1L, "spotId", 5L);

        analyticsService.handleBookingEvent(payload);

        then(analyticsRepo).should().save(argThat(log ->
                log.getTimestamp() != null &&
                !log.getTimestamp().isBefore(before)));
    }
}
