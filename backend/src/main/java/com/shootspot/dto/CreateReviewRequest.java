package com.shootspot.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReviewRequest(
        @NotNull(message = "Choose a rating from 1 to 5 stars")
        @Min(value = 1, message = "Rating must be at least 1 star")
        @Max(value = 5, message = "Rating cannot exceed 5 stars")
        Integer rating,
        @NotBlank(message = "Review text is required")
        @Size(min = 3, max = 500, message = "Review must be between 3 and 500 characters")
        String comment) {
}
