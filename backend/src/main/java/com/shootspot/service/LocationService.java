package com.shootspot.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.shootspot.entity.Location;
import com.shootspot.repository.LocationRepository;
import com.shootspot.repository.LocationRatingSummary;
import com.shootspot.repository.ReviewRepository;

@Service
public class LocationService {

    private final LocationRepository locationRepository;
    private final ReviewRepository reviewRepository;

    public LocationService(LocationRepository locationRepository, ReviewRepository reviewRepository) {
        this.locationRepository = locationRepository;
        this.reviewRepository = reviewRepository;
    }

    public List<Location> getAllLocations() {
        List<Location> locations = locationRepository.findAll();
        applyRatingSummaries(locations);
        return locations;
    }

    public Location getLocationById(Long id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Location not found with id: " + id));
        applyRatingSummaries(List.of(location));
        return location;
    }

    public Location createLocation(Location location) {
        return locationRepository.save(location);
    }

    public Location updateLocation(Long id, Location updatedLocation) {
        Location existingLocation = getLocationById(id);
        existingLocation.setName(updatedLocation.getName());
        existingLocation.setDescription(updatedLocation.getDescription());
        existingLocation.setAddress(updatedLocation.getAddress());
        existingLocation.setCity(updatedLocation.getCity());
        existingLocation.setCategory(updatedLocation.getCategory());
        existingLocation.setPricePerHour(updatedLocation.getPricePerHour());
        existingLocation.setImageUrl(updatedLocation.getImageUrl());
        existingLocation.setAvailable(updatedLocation.isAvailable());
        existingLocation.setBookable(updatedLocation.isBookable());
        return locationRepository.save(existingLocation);
    }

    public void deleteLocation(Long id) {
        Location location = getLocationById(id);
        locationRepository.delete(location);
    }

    private void applyRatingSummaries(List<Location> locations) {
        if (locations.isEmpty()) {
            return;
        }
        Map<Long, LocationRatingSummary> summaries = reviewRepository.findLocationRatingSummaries()
                .stream()
                .collect(Collectors.toMap(
                        LocationRatingSummary::getLocationId,
                        Function.identity()));
        for (Location location : locations) {
            LocationRatingSummary summary = summaries.get(location.getId());
            location.setAverageRating(summary == null ? null : summary.getAverageRating());
            location.setReviewCount(summary == null ? 0L : summary.getReviewCount());
        }
    }
}
