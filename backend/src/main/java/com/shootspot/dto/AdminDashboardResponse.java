package com.shootspot.dto;

public record AdminDashboardResponse(
        long totalLocations,
        long totalBookings,
        long pendingBookings,
        long confirmedBookings) {
}
