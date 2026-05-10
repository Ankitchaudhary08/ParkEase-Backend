package com.parkease.booking.repository;

import com.parkease.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);
    List<Booking> findByLotId(Long lotId);
    List<Booking> findBySpotId(Long spotId);
    List<Booking> findByStatus(Booking.BookingStatus status);
    List<Booking> findByVehiclePlate(String vehiclePlate);
    Optional<Booking> findBySpotIdAndStatus(Long spotId, Booking.BookingStatus status);
    int countByLotIdAndStatus(Long lotId, Booking.BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.status = 'RESERVED' AND b.startTime < :cutoff AND b.bookingType = 'PRE_BOOKING'")
    List<Booking> findExpiredPreBookings(@Param("cutoff") LocalDateTime cutoff);

    List<Booking> findByLotIdOrderByCreatedAtDesc(Long lotId);
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);
}
