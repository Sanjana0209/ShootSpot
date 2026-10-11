package com.shootspot.dto;

import java.time.LocalTime;
import java.util.List;

public record BookingAvailabilityResponse(
        List<OccupiedInterval> occupiedIntervals,
        boolean requestedSlotAvailable,
        LocalTime nextAvailableTime) {

    public record OccupiedInterval(LocalTime startTime, LocalTime endTime) {
    }
}
