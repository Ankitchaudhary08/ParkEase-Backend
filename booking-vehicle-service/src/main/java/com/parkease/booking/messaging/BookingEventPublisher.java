package com.parkease.booking.messaging;

import com.parkease.booking.entity.Booking;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BookingEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public static final String BOOKING_EXCHANGE = "parkease.booking.exchange";
    public static final String BOOKING_CREATED_KEY = "booking.created";
    public static final String BOOKING_CANCELLED_KEY = "booking.cancelled";
    public static final String BOOKING_CHECKIN_KEY = "booking.checkin";
    public static final String BOOKING_CHECKOUT_KEY = "booking.checkout";
    public static final String BOOKING_EXTENDED_KEY = "booking.extended";

    public void publishBookingCreated(Booking booking) {
        rabbitTemplate.convertAndSend(BOOKING_EXCHANGE, BOOKING_CREATED_KEY, toPayload(booking));
    }

    public void publishBookingCancelled(Booking booking) {
        rabbitTemplate.convertAndSend(BOOKING_EXCHANGE, BOOKING_CANCELLED_KEY, toPayload(booking));
    }

    public void publishCheckIn(Booking booking) {
        rabbitTemplate.convertAndSend(BOOKING_EXCHANGE, BOOKING_CHECKIN_KEY, toPayload(booking));
    }

    public void publishCheckOut(Booking booking) {
        rabbitTemplate.convertAndSend(BOOKING_EXCHANGE, BOOKING_CHECKOUT_KEY, toPayload(booking));
    }

    public void publishExtended(Booking booking) {
        rabbitTemplate.convertAndSend(BOOKING_EXCHANGE, BOOKING_EXTENDED_KEY, toPayload(booking));
    }

    private Map<String, Object> toPayload(Booking booking) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("bookingId", booking.getBookingId());
        payload.put("userId", booking.getUserId());
        payload.put("lotId", booking.getLotId());
        payload.put("spotId", booking.getSpotId());
        payload.put("vehiclePlate", booking.getVehiclePlate());
        payload.put("status", booking.getStatus().name());
        payload.put("startTime", booking.getStartTime() != null ? booking.getStartTime().toString() : null);
        payload.put("endTime", booking.getEndTime() != null ? booking.getEndTime().toString() : null);
        payload.put("checkInTime", booking.getCheckInTime() != null ? booking.getCheckInTime().toString() : null);
        payload.put("checkOutTime", booking.getCheckOutTime() != null ? booking.getCheckOutTime().toString() : null);
        payload.put("totalAmount", booking.getTotalAmount());
        payload.put("bookingType", booking.getBookingType().name());
        return payload;
    }
}
