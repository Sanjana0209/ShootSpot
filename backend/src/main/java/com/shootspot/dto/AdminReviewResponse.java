package com.shootspot.dto;

import java.time.LocalDateTime;

public record AdminReviewResponse(
        Long id,
        Long bookingId,
        Long locationId,
        String locationName,
        String customerName,
        String customerEmail,
        int rating,
        String comment,
        LocalDateTime createdAt) {
}
