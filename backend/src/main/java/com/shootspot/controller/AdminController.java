package com.shootspot.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.shootspot.dto.AdminBookingResponse;
import com.shootspot.dto.AdminDashboardResponse;
import com.shootspot.dto.AdminReviewResponse;
import com.shootspot.dto.UserResponse;
import com.shootspot.dto.UpdateBookingStatusRequest;
import com.shootspot.repository.BookingRepository;
import com.shootspot.repository.LocationRepository;
import com.shootspot.repository.UserRepository;
import com.shootspot.service.BookingService;
import com.shootspot.service.ReviewService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final BookingRepository bookingRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final BookingService bookingService;
    private final ReviewService reviewService;

    public AdminController(
            BookingRepository bookingRepository,
            LocationRepository locationRepository,
            UserRepository userRepository,
            BookingService bookingService,
            ReviewService reviewService) {
        this.bookingRepository = bookingRepository;
        this.locationRepository = locationRepository;
        this.userRepository = userRepository;
        this.bookingService = bookingService;
        this.reviewService = reviewService;
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() {
        return new AdminDashboardResponse(
                locationRepository.count(),
                bookingRepository.count(),
                bookingRepository.countByStatus("PENDING"),
                bookingRepository.countByStatus("CONFIRMED"));
    }

    @GetMapping("/bookings")
    public List<AdminBookingResponse> bookings() {
        return bookingService.getAllBookingsForAdmin();
    }

    @GetMapping("/users")
    public List<UserResponse> users() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole(),
                        user.getPhone()))
                .toList();
    }

    @GetMapping("/reviews")
    public List<AdminReviewResponse> reviews() {
        return reviewService.getReviewsForAdmin();
    }

    @DeleteMapping("/reviews/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeReview(@PathVariable Long id) {
        reviewService.removeReview(id);
    }

    @PatchMapping("/bookings/{id}/status")
    @ResponseStatus(HttpStatus.OK)
    public AdminBookingResponse updateBookingStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookingStatusRequest request) {
        return bookingService.updateBookingStatus(id, request.status());
    }
}
