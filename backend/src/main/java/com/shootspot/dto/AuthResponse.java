package com.shootspot.dto;

public record AuthResponse(String tokenType, String token, UserResponse user) {
}
