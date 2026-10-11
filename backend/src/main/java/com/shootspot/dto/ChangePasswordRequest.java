package com.shootspot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,
        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 16, message = "Password must be between 8 and 16 characters")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).+$",
                message = "Password must include uppercase, lowercase, a number, and a special character")
        String newPassword,
        @NotBlank(message = "Please confirm your new password")
        String confirmNewPassword) {
}
