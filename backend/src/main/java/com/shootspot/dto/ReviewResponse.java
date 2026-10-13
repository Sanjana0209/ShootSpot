package com.shootspot.dto;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long bookingId,
        Long locationId,
        String authorName,
        int rating,
        String comment,
        LocalDateTime createdAt) {
}
