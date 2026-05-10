package com.parkease.booking.service;

import com.parkease.booking.client.ParkingServiceClient;
import com.parkease.booking.dto.CreateBookingRequest;
import com.parkease.booking.entity.Booking;
import com.parkease.booking.entity.Vehicle;
import com.parkease.booking.messaging.BookingEventPublisher;
import com.parkease.booking.repository.BookingRepository;
import com.parkease.booking.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock VehicleRepository vehicleRepository;
    @Mock ParkingServiceClient parkingClient;
    @Mock BookingEventPublisher eventPublisher;

    @InjectMocks BookingService bookingService;

    Booking reservedBooking;
    Booking activeBooking;
    LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        reservedBooking = Booking.builder()
                .bookingId(1L).userId(10L).lotId(2L).spotId(3L)
                .vehiclePlate("MH01AB1234")
                .vehicleType(Vehicle.VehicleType.FOUR_WHEELER)
                .bookingType(Booking.BookingType.PRE_BOOKING)
                .startTime(now).endTime(now.plusHours(2))
                .pricePerHour(50.0)
                .status(Booking.BookingStatus.RESERVED)
                .build();

        activeBooking = Booking.builder()
                .bookingId(2L).userId(10L).lotId(2L).spotId(3L)
                .vehiclePlate("MH01AB1234")
                .vehicleType(Vehicle.VehicleType.FOUR_WHEELER)
                .bookingType(Booking.BookingType.WALK_IN)
                .startTime(now.minusHours(2)).endTime(now.plusHours(1))
                .checkInTime(now.minusHours(1))
                .pricePerHour(50.0)
                .status(Booking.BookingStatus.ACTIVE)
                .build();
    }

    // ── createBooking ────────────────────────────────────────────────────────

    @Test
    void createBooking_success_withKnownVehicle() {
        CreateBookingRequest req = CreateBookingRequest.builder()
                .lotId(2L).spotId(3L).vehiclePlate("MH01AB1234")
                .bookingType(Booking.BookingType.PRE_BOOKING)
                .startTime(now).endTime(now.plusHours(2))
                .pricePerHour(50.0).build();

        given(vehicleRepository.findByLicensePlate("MH01AB1234"))
                .willReturn(Optional.of(Vehicle.builder()
                        .vehicleType(Vehicle.VehicleType.FOUR_WHEELER).build()));
        given(bookingRepository.save(any(Booking.class))).willReturn(reservedBooking);

        Booking result = bookingService.createBooking(req, 10L);

        assertThat(result.getStatus()).isEqualTo(Booking.BookingStatus.RESERVED);
        then(parkingClient).should().reserveSpot(3L);
        then(parkingClient).should().decrementAvailable(2L);
        then(eventPublisher).should().publishBookingCreated(reservedBooking);
    }

    @Test
    void createBooking_usesFallbackVehicleType_fromRequest_whenPlateUnknown() {
        CreateBookingRequest req = CreateBookingRequest.builder()
                .lotId(2L).spotId(3L).vehiclePlate("DL99ZZ0000")
                .vehicleType(Vehicle.VehicleType.TWO_WHEELER)
                .bookingType(Booking.BookingType.WALK_IN)
                .startTime(now).endTime(now.plusHours(1))
                .pricePerHour(30.0).build();

        given(vehicleRepository.findByLicensePlate("DL99ZZ0000")).willReturn(Optional.empty());
        given(bookingRepository.save(any(Booking.class))).willAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.createBooking(req, 5L);

        assertThat(result.getVehicleType()).isEqualTo(Vehicle.VehicleType.TWO_WHEELER);
    }

    @Test
    void createBooking_defaultsToFourWheeler_whenNoVehicleInfoAvailable() {
        CreateBookingRequest req = CreateBookingRequest.builder()
                .lotId(2L).spotId(3L).vehiclePlate("UK01AB0000")
                .bookingType(Booking.BookingType.WALK_IN)
                .startTime(now).endTime(now.plusHours(1))
                .pricePerHour(30.0).build();

        given(vehicleRepository.findByLicensePlate("UK01AB0000")).willReturn(Optional.empty());
        given(bookingRepository.save(any(Booking.class))).willAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.createBooking(req, 5L);

        assertThat(result.getVehicleType()).isEqualTo(Vehicle.VehicleType.FOUR_WHEELER);
    }

    // ── checkIn ──────────────────────────────────────────────────────────────

    @Test
    void checkIn_success_setsActiveStatusAndCheckInTime() {
        given(bookingRepository.findById(1L)).willReturn(Optional.of(reservedBooking));
        given(bookingRepository.save(any(Booking.class))).willAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.checkIn(1L);

        assertThat(result.getStatus()).isEqualTo(Booking.BookingStatus.ACTIVE);
        assertThat(result.getCheckInTime()).isNotNull();
        then(parkingClient).should().occupySpot(3L);
        then(eventPublisher).should().publishCheckIn(any(Booking.class));
    }

    @Test
    void checkIn_throwsIllegalState_whenBookingNotReserved() {
        given(bookingRepository.findById(2L)).willReturn(Optional.of(activeBooking));

        assertThatThrownBy(() -> bookingService.checkIn(2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RESERVED");
    }

    // ── checkOut ─────────────────────────────────────────────────────────────

    @Test
    void checkOut_success_setsCompletedAndCalculatesAmount() {
        given(bookingRepository.findById(2L)).willReturn(Optional.of(activeBooking));
        given(bookingRepository.save(any(Booking.class))).willAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.checkOut(2L);

        assertThat(result.getStatus()).isEqualTo(Booking.BookingStatus.COMPLETED);
        assertThat(result.getTotalAmount()).isGreaterThan(0.0);
        assertThat(result.getCheckOutTime()).isNotNull();
        then(parkingClient).should().releaseSpot(3L);
        then(parkingClient).should().incrementAvailable(2L);
        then(eventPublisher).should().publishCheckOut(any(Booking.class));
    }

    @Test
    void checkOut_throwsIllegalState_whenBookingNotActive() {
        given(bookingRepository.findById(1L)).willReturn(Optional.of(reservedBooking));

        assertThatThrownBy(() -> bookingService.checkOut(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ACTIVE");
    }

    @Test
    void checkOut_chargesMinimumOneHour_whenDurationIsLessThan60Minutes() {
        activeBooking.setCheckInTime(now.minusMinutes(10)); // only 10 minutes
        given(bookingRepository.findById(2L)).willReturn(Optional.of(activeBooking));
        given(bookingRepository.save(any(Booking.class))).willAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.checkOut(2L);

        // min 1 hr * 50.0/hr = 50.0
        assertThat(result.getTotalAmount()).isGreaterThanOrEqualTo(50.0);
    }

    @Test
    void checkOut_chargesCorrectly_forTwoHourStay() {
        activeBooking.setCheckInTime(now.minusHours(2));
        given(bookingRepository.findById(2L)).willReturn(Optional.of(activeBooking));
        given(bookingRepository.save(any(Booking.class))).willAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.checkOut(2L);

        assertThat(result.getTotalAmount()).isCloseTo(100.0, within(2.0));
    }

    // ── cancelBooking ────────────────────────────────────────────────────────

    @Test
    void cancelBooking_success_fromReservedState() {
        given(bookingRepository.findById(1L)).willReturn(Optional.of(reservedBooking));
        given(bookingRepository.save(any(Booking.class))).willAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.cancelBooking(1L);

        assertThat(result.getStatus()).isEqualTo(Booking.BookingStatus.CANCELLED);
        then(parkingClient).should().releaseSpot(3L);
        then(parkingClient).should().incrementAvailable(2L);
        then(eventPublisher).should().publishBookingCancelled(any(Booking.class));
    }

    @Test
    void cancelBooking_throwsIllegalState_whenBookingAlreadyCompleted() {
        Booking completed = Booking.builder()
                .bookingId(9L).status(Booking.BookingStatus.COMPLETED).build();
        given(bookingRepository.findById(9L)).willReturn(Optional.of(completed));

        assertThatThrownBy(() -> bookingService.cancelBooking(9L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot cancel a completed booking");
    }

    // ── extendBooking ────────────────────────────────────────────────────────

    @Test
    void extendBooking_success_updatesEndTime_fromActive() {
        LocalDateTime newEnd = now.plusHours(4);
        given(bookingRepository.findById(2L)).willReturn(Optional.of(activeBooking));
        given(bookingRepository.save(any(Booking.class))).willAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.extendBooking(2L, newEnd);

        assertThat(result.getEndTime()).isEqualTo(newEnd);
        then(eventPublisher).should().publishExtended(any(Booking.class));
    }

    @Test
    void extendBooking_success_updatesEndTime_fromReserved() {
        LocalDateTime newEnd = now.plusHours(5);
        given(bookingRepository.findById(1L)).willReturn(Optional.of(reservedBooking));
        given(bookingRepository.save(any(Booking.class))).willAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.extendBooking(1L, newEnd);

        assertThat(result.getEndTime()).isEqualTo(newEnd);
    }

    @Test
    void extendBooking_throwsIllegalState_whenCancelledOrCompleted() {
        Booking cancelled = Booking.builder()
                .bookingId(7L).status(Booking.BookingStatus.CANCELLED).build();
        given(bookingRepository.findById(7L)).willReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> bookingService.extendBooking(7L, now.plusHours(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ACTIVE or RESERVED");
    }

    // ── calculateAmount ──────────────────────────────────────────────────────

    @Test
    void calculateAmount_usesCheckOutTime_whenPresent() {
        Booking completed = Booking.builder()
                .bookingId(3L)
                .checkInTime(now.minusHours(3))
                .checkOutTime(now)
                .pricePerHour(60.0)
                .build();
        given(bookingRepository.findById(3L)).willReturn(Optional.of(completed));

        double amount = bookingService.calculateAmount(3L);

        assertThat(amount).isCloseTo(180.0, within(1.0));
    }

    @Test
    void calculateAmount_usesStartTime_whenNoCheckInRecorded() {
        Booking booking = Booking.builder()
                .bookingId(4L)
                .startTime(now.minusHours(1))
                .pricePerHour(50.0)
                .build();
        given(bookingRepository.findById(4L)).willReturn(Optional.of(booking));

        double amount = bookingService.calculateAmount(4L);

        assertThat(amount).isGreaterThanOrEqualTo(50.0);
    }

    @Test
    void calculateAmount_appliesMinimumOneHour() {
        Booking booking = Booking.builder()
                .bookingId(5L)
                .checkInTime(now.minusMinutes(5))
                .checkOutTime(now)
                .pricePerHour(80.0)
                .build();
        given(bookingRepository.findById(5L)).willReturn(Optional.of(booking));

        double amount = bookingService.calculateAmount(5L);

        assertThat(amount).isGreaterThanOrEqualTo(80.0);
    }

    // ── getters ──────────────────────────────────────────────────────────────

    @Test
    void getBookingById_throwsRuntimeException_whenNotFound() {
        given(bookingRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getBookingById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Booking not found: 999");
    }

    @Test
    void getBookingsByUser_returnsOrderedList() {
        given(bookingRepository.findByUserIdOrderByCreatedAtDesc(10L))
                .willReturn(List.of(activeBooking, reservedBooking));

        List<Booking> result = bookingService.getBookingsByUser(10L);

        assertThat(result).hasSize(2);
    }

    @Test
    void getActiveBookings_filtersOnlyActiveAndReserved() {
        Booking completed = Booking.builder()
                .bookingId(99L).lotId(2L).status(Booking.BookingStatus.COMPLETED).build();

        given(bookingRepository.findByLotId(2L))
                .willReturn(List.of(reservedBooking, activeBooking, completed));

        List<Booking> result = bookingService.getActiveBookings(2L);

        assertThat(result).hasSize(2)
                .noneMatch(b -> b.getStatus() == Booking.BookingStatus.COMPLETED);
    }
}
