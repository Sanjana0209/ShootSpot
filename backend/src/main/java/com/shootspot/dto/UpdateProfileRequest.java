package com.shootspot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 3, max = 30, message = "Name must be between 3 and 30 characters")
        @Pattern(regexp = "^[\\p{L} ]+$", message = "Name may contain letters and spaces only")
        String name,
        @NotBlank(message = "Mobile number is required")
        @Pattern(
                regexp = "^[6-9][0-9]{9}$",
                message = "Enter a valid 10-digit Indian mobile number")
        String phone) {

    public UpdateProfileRequest {
        name = name == null ? null : name.trim();
    }
}
