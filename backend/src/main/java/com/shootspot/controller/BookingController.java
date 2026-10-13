package com.shootspot.controller;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.shootspot.dto.BookingResponse;
import com.shootspot.dto.BookingAvailabilityResponse;
import com.shootspot.dto.CreateBookingRequest;
import com.shootspot.dto.CreateReviewRequest;
import com.shootspot.dto.ReviewResponse;
import com.shootspot.service.BookingService;
import com.shootspot.service.ReviewService;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final ReviewService reviewService;

    public BookingController(BookingService bookingService, ReviewService reviewService) {
        this.bookingService = bookingService;
        this.reviewService = reviewService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return bookingService.createBooking(request, userDetails.getUsername());
    }

    @GetMapping("/me")
    public List<BookingResponse> currentUserBookings(
            @AuthenticationPrincipal UserDetails userDetails) {
        return bookingService.getCurrentUserBookings(userDetails.getUsername());
    }

    @GetMapping("/availability")
    public BookingAvailabilityResponse availability(
            @RequestParam Long locationId,
            @RequestParam LocalDate bookingDate,
            @RequestParam LocalTime startTime,
            @RequestParam int durationHours) {
        return bookingService.getAvailability(locationId, bookingDate, startTime, durationHours);
    }

    @PostMapping("/{bookingId}/review")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse createReview(
            @PathVariable Long bookingId,
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return reviewService.createReview(bookingId, request, userDetails.getUsername());
    }
}
