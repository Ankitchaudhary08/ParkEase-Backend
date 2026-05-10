package com.parkease.booking.service;

import com.parkease.booking.client.ParkingServiceClient;
import com.parkease.booking.dto.CreateBookingRequest;
import com.parkease.booking.entity.Booking;
import com.parkease.booking.entity.Vehicle;
import com.parkease.booking.messaging.BookingEventPublisher;
import com.parkease.booking.repository.BookingRepository;
import com.parkease.booking.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private final ParkingServiceClient parkingClient;
    private final BookingEventPublisher eventPublisher;

    private static final long GRACE_PERIOD_MINUTES = 30;

    @Transactional
    public Booking createBooking(CreateBookingRequest request, Long userId) {
        Vehicle.VehicleType vehicleType = vehicleRepository.findByLicensePlate(request.getVehiclePlate())
                .map(Vehicle::getVehicleType)
                .orElse(request.getVehicleType() != null ? request.getVehicleType() : Vehicle.VehicleType.FOUR_WHEELER);

        parkingClient.reserveSpot(request.getSpotId());
        parkingClient.decrementAvailable(request.getLotId());

        Booking booking = Booking.builder()
                .userId(userId)
                .lotId(request.getLotId())
                .spotId(request.getSpotId())
                .vehiclePlate(request.getVehiclePlate())
                .vehicleType(vehicleType)
                .bookingType(request.getBookingType())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .pricePerHour(request.getPricePerHour())
                .status(Booking.BookingStatus.RESERVED)
                .build();

        Booking saved = bookingRepository.save(booking);
        eventPublisher.publishBookingCreated(saved);
        return saved;
    }

    @Transactional
    public Booking checkIn(Long bookingId) {
        Booking booking = getBookingById(bookingId);
        if (booking.getStatus() != Booking.BookingStatus.RESERVED) {
            throw new IllegalStateException("Booking is not in RESERVED state");
        }
        booking.setCheckInTime(LocalDateTime.now());
        booking.setStatus(Booking.BookingStatus.ACTIVE);
        parkingClient.occupySpot(booking.getSpotId());
        Booking saved = bookingRepository.save(booking);
        eventPublisher.publishCheckIn(saved);
        return saved;
    }

    @Transactional
    public Booking checkOut(Long bookingId) {
        Booking booking = getBookingById(bookingId);
        if (booking.getStatus() != Booking.BookingStatus.ACTIVE) {
            throw new IllegalStateException("Booking is not ACTIVE");
        }
        LocalDateTime checkOut = LocalDateTime.now();
        booking.setCheckOutTime(checkOut);
        booking.setStatus(Booking.BookingStatus.COMPLETED);
        double hours = Math.max(1.0, Duration.between(booking.getCheckInTime(), checkOut).toMinutes() / 60.0);
        booking.setTotalAmount(Math.round(hours * booking.getPricePerHour() * 100.0) / 100.0);
        parkingClient.releaseSpot(booking.getSpotId());
        parkingClient.incrementAvailable(booking.getLotId());
        Booking saved = bookingRepository.save(booking);
        eventPublisher.publishCheckOut(saved);
        return saved;
    }

    @Transactional
    public Booking cancelBooking(Long bookingId) {
        Booking booking = getBookingById(bookingId);
        if (booking.getStatus() == Booking.BookingStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed booking");
        }
        booking.setStatus(Booking.BookingStatus.CANCELLED);
        parkingClient.releaseSpot(booking.getSpotId());
        parkingClient.incrementAvailable(booking.getLotId());
        Booking saved = bookingRepository.save(booking);
        eventPublisher.publishBookingCancelled(saved);
        return saved;
    }

    @Transactional
    public Booking extendBooking(Long bookingId, LocalDateTime newEndTime) {
        Booking booking = getBookingById(bookingId);
        if (booking.getStatus() != Booking.BookingStatus.ACTIVE && booking.getStatus() != Booking.BookingStatus.RESERVED) {
            throw new IllegalStateException("Can only extend ACTIVE or RESERVED bookings");
        }
        booking.setEndTime(newEndTime);
        Booking saved = bookingRepository.save(booking);
        eventPublisher.publishExtended(saved);
        return saved;
    }

    public double calculateAmount(Long bookingId) {
        Booking booking = getBookingById(bookingId);
        LocalDateTime end = booking.getCheckOutTime() != null ? booking.getCheckOutTime() : LocalDateTime.now();
        LocalDateTime start = booking.getCheckInTime() != null ? booking.getCheckInTime() : booking.getStartTime();
        double hours = Math.max(1.0, Duration.between(start, end).toMinutes() / 60.0);
        return Math.round(hours * booking.getPricePerHour() * 100.0) / 100.0;
    }

    public Booking getBookingById(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));
    }

    public List<Booking> getBookingsByUser(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Booking> getBookingsByLot(Long lotId) {
        return bookingRepository.findByLotIdOrderByCreatedAtDesc(lotId);
    }

    public List<Booking> getActiveBookings(Long lotId) {
        return bookingRepository.findByLotId(lotId).stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.ACTIVE || b.getStatus() == Booking.BookingStatus.RESERVED)
                .toList();
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void autoExpirePreBookings() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(GRACE_PERIOD_MINUTES);
        List<Booking> expired = bookingRepository.findExpiredPreBookings(cutoff);
        expired.forEach(b -> {
            try {
                cancelBooking(b.getBookingId());
                log.info("Auto-cancelled expired pre-booking: {}", b.getBookingId());
            } catch (Exception e) {
                log.error("Failed to auto-cancel booking {}: {}", b.getBookingId(), e.getMessage());
            }
        });
    }
}
