package com.shootspot.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 3, max = 30, message = "Name must be between 3 and 30 characters")
        @Pattern(regexp = "^[\\p{L} ]+$", message = "Name may contain letters and spaces only")
        String name,
        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 254, message = "Email must be 254 characters or fewer")
        @Pattern(
                regexp = "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$",
                message = "Enter a valid email address")
        String email,
        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Enter a valid 10-digit Indian mobile number")
        String phone,
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 16, message = "Password must be between 8 and 16 characters")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).+$",
                message = "Password must include uppercase, lowercase, a number, and a special character")
        String password) {

    public RegisterRequest {
        name = name == null ? null : name.trim();
        email = email == null ? null : email.trim();
    }
}
