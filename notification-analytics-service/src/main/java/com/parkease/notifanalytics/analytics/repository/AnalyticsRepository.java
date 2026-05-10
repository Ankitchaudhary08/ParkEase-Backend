package com.parkease.notifanalytics.analytics.repository;

import com.parkease.notifanalytics.analytics.entity.OccupancyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnalyticsRepository extends JpaRepository<OccupancyLog, Long> {
    List<OccupancyLog> findByLotId(Long lotId);

    List<OccupancyLog> findByLotIdAndTimestampBetween(Long lotId, LocalDateTime from, LocalDateTime to);

    @Query("SELECT AVG(o.occupancyRate) FROM OccupancyLog o WHERE o.lotId = :lotId")
    Double avgOccupancyByLotId(@Param("lotId") Long lotId);

    @Query("SELECT HOUR(o.timestamp) as hr, AVG(o.occupancyRate) as avgRate FROM OccupancyLog o WHERE o.lotId = :lotId GROUP BY HOUR(o.timestamp) ORDER BY hr")
    List<Object[]> getHourlyOccupancy(@Param("lotId") Long lotId);

    @Query("SELECT HOUR(o.timestamp) as hr, COUNT(o) as cnt FROM OccupancyLog o WHERE o.lotId = :lotId GROUP BY HOUR(o.timestamp) ORDER BY cnt DESC")
    List<Object[]> getPeakHours(@Param("lotId") Long lotId);

    @Query("SELECT o.spotType, COUNT(o) FROM OccupancyLog o WHERE o.lotId = :lotId GROUP BY o.spotType")
    List<Object[]> getSpotTypeUtilisation(@Param("lotId") Long lotId);

    int countByLotIdAndTimestampAfter(Long lotId, LocalDateTime after);
}
