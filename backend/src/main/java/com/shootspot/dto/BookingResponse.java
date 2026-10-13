package com.shootspot.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record BookingResponse(
        Long id,
        Long locationId,
        String locationName,
        String locationCity,
        LocalDate bookingDate,
        LocalTime startTime,
        LocalTime endTime,
        int durationHours,
        BigDecimal totalPrice,
        String status,
        boolean reviewed) {
}
