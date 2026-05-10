package com.parkease.booking.resource;

import com.parkease.booking.dto.CreateBookingRequest;
import com.parkease.booking.entity.Booking;
import com.parkease.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Parking spot reservation and lifecycle management")
public class BookingResource {

    private final BookingService bookingService;

    @PostMapping
    @Operation(summary = "Create a new booking")
    public ResponseEntity<Booking> create(
            @Valid @RequestBody CreateBookingRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(bookingService.createBooking(request, userId));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<Booking> getById(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.getBookingById(bookingId));
    }

    @GetMapping("/user")
    @Operation(summary = "Get all bookings for authenticated user")
    public ResponseEntity<List<Booking>> getByUser(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(bookingService.getBookingsByUser(userId));
    }

    @GetMapping("/lot/{lotId}")
    @Operation(summary = "Get all bookings for a specific lot")
    public ResponseEntity<List<Booking>> getByLot(@PathVariable Long lotId) {
        return ResponseEntity.ok(bookingService.getBookingsByLot(lotId));
    }

    @GetMapping("/lot/{lotId}/active")
    @Operation(summary = "Get active bookings for a lot")
    public ResponseEntity<List<Booking>> getActive(@PathVariable Long lotId) {
        return ResponseEntity.ok(bookingService.getActiveBookings(lotId));
    }

    @PutMapping("/{bookingId}/checkin")
    @Operation(summary = "Driver check-in — marks spot OCCUPIED")
    public ResponseEntity<Booking> checkIn(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.checkIn(bookingId));
    }

    @PutMapping("/{bookingId}/checkout")
    @Operation(summary = "Driver check-out — computes fare and publishes payment event")
    public ResponseEntity<Booking> checkOut(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.checkOut(bookingId));
    }

    @PutMapping("/{bookingId}/cancel")
    @Operation(summary = "Cancel a booking and release the spot")
    public ResponseEntity<Booking> cancel(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.cancelBooking(bookingId));
    }

    @PutMapping("/{bookingId}/extend")
    @Operation(summary = "Extend booking end time")
    public ResponseEntity<Booking> extend(
            @PathVariable Long bookingId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(bookingService.extendBooking(bookingId, LocalDateTime.parse(body.get("newEndTime"))));
    }

    @GetMapping("/{bookingId}/amount")
    @Operation(summary = "Calculate current fare for a booking")
    public ResponseEntity<Double> calculateAmount(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.calculateAmount(bookingId));
    }
}
