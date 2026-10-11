package com.shootspot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateBookingStatusRequest(
        @NotBlank(message = "Booking status is required")
        @Pattern(
                regexp = "^(PENDING|CONFIRMED|CANCELLED|COMPLETED)$",
                message = "Status must be PENDING, CONFIRMED, CANCELLED, or COMPLETED")
        String status) {
}
