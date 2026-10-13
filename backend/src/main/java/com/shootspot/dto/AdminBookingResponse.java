package com.shootspot.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record AdminBookingResponse(
        Long id,
        Long locationId,
        String locationName,
        String locationCity,
        String customerName,
        String customerEmail,
        String customerPhone,
        LocalDate bookingDate,
        LocalTime startTime,
        LocalTime endTime,
        int durationHours,
        BigDecimal totalPrice,
        String status) {
}
