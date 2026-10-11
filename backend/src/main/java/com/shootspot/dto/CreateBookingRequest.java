package com.shootspot.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateBookingRequest(
        @NotNull(message = "Location is required")
        @Positive(message = "Select a valid location")
        Long locationId,
        @NotNull(message = "Booking date is required")
        @FutureOrPresent(message = "Booking date cannot be in the past")
        LocalDate bookingDate,
        @NotNull(message = "Start time is required")
        LocalTime startTime,
        @NotNull(message = "Duration is required")
        @Min(value = 1, message = "Duration must be at least 1 hour")
        @Max(value = 12, message = "Duration cannot exceed 12 hours")
        Integer durationHours) {
}
