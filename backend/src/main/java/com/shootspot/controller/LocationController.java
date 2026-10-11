package com.shootspot.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.shootspot.entity.Location;
import com.shootspot.dto.CreateReviewRequest;
import com.shootspot.dto.ReviewResponse;
import com.shootspot.service.LocationService;
import com.shootspot.service.ReviewService;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService locationService;
    private final ReviewService reviewService;

    public LocationController(LocationService locationService, ReviewService reviewService) {
        this.locationService = locationService;
        this.reviewService = reviewService;
    }

    @GetMapping
    public List<Location> getAllLocations() {
        return locationService.getAllLocations();
    }

    @GetMapping("/{id}")
    public Location getLocationById(@PathVariable Long id) {
        return locationService.getLocationById(id);
    }

    @GetMapping("/{id}/reviews")
    public List<ReviewResponse> getLocationReviews(@PathVariable Long id) {
        return reviewService.getLocationReviews(id);
    }

    @PostMapping("/{id}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse createLocationReview(
            @PathVariable Long id,
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return reviewService.createLocationReview(id, request, userDetails.getUsername());
    }

    @PostMapping
    public ResponseEntity<Location> createLocation(@Valid @RequestBody Location location) {
        Location createdLocation = locationService.createLocation(location);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdLocation);
    }

    @PutMapping("/{id}")
    public Location updateLocation(
            @PathVariable Long id, @Valid @RequestBody Location location) {
        return locationService.updateLocation(id, location);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocation(@PathVariable Long id) {
        locationService.deleteLocation(id);
        return ResponseEntity.noContent().build();
    }
}
