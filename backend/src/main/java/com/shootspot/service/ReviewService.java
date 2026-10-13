package com.shootspot.service;

import java.util.List;
import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.hibernate.exception.ConstraintViolationException;

import com.shootspot.dto.AdminReviewResponse;
import com.shootspot.dto.CreateReviewRequest;
import com.shootspot.dto.ReviewResponse;
import com.shootspot.entity.Booking;
import com.shootspot.entity.Location;
import com.shootspot.entity.Review;
import com.shootspot.entity.User;
import com.shootspot.repository.BookingRepository;
import com.shootspot.repository.LocationRepository;
import com.shootspot.repository.ReviewRepository;
import com.shootspot.repository.UserRepository;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;

    public ReviewService(
            ReviewRepository reviewRepository,
            BookingRepository bookingRepository,
            LocationRepository locationRepository,
            UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.locationRepository = locationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReviewResponse createLocationReview(
            Long locationId, CreateReviewRequest request, String authenticatedEmail) {
        User user = requireUserAccount(authenticatedEmail);
        Location location = locationRepository.findByIdForUpdate(locationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Location not found"));
        if (reviewRepository.existsByUser_IdAndLocation_Id(user.getId(), locationId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "You have already reviewed this location");
        }

        Review review = new Review();
        review.setUser(user);
        review.setLocation(location);
        review.setRating(request.rating());
        review.setComment(normalizeComment(request.comment()));
        return saveReview(review, "You have already reviewed this location");
    }

    @Transactional
    public ReviewResponse createReview(
            Long bookingId, CreateReviewRequest request, String authenticatedEmail) {
        User user = requireUserAccount(authenticatedEmail);
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found"));

        if (!Objects.equals(booking.getUser().getId(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        }
        if (!"COMPLETED".equals(booking.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "A review can be submitted only after the booking is completed");
        }
        Location location = locationRepository.findByIdForUpdate(booking.getLocation().getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Location not found"));
        if (reviewRepository.existsByBooking_Id(bookingId)
                || reviewRepository.existsByUser_IdAndLocation_Id(user.getId(), location.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "You have already reviewed this location");
        }

        Review review = new Review();
        review.setBooking(booking);
        review.setUser(user);
        review.setLocation(location);
        review.setRating(request.rating());
        review.setComment(normalizeComment(request.comment()));
        return saveReview(review, "You have already reviewed this location");
    }

    private ReviewResponse saveReview(Review review, String duplicateMessage) {
        try {
            return toResponse(reviewRepository.saveAndFlush(review));
        } catch (DataIntegrityViolationException exception) {
            if (!isDuplicateReviewConstraint(exception)) {
                throw exception;
            }
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, duplicateMessage);
        }
    }

    private boolean isDuplicateReviewConstraint(DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException constraintViolation) {
                String constraintName = constraintViolation.getConstraintName();
                return "uk_review_booking".equals(constraintName)
                        || "uq_reviews_location_user_without_booking".equals(constraintName);
            }
        }
        return false;
    }

    private User requireUserAccount(String authenticatedEmail) {
        User user = userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authentication required"));
        if (!"USER".equals(user.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Only user accounts can submit reviews");
        }
        return user;
    }

    private String normalizeComment(String comment) {
        String normalized = comment.trim();
        if (normalized.length() < 3) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Review must contain at least 3 non-space characters");
        }
        return normalized;
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getLocationReviews(Long locationId) {
        return reviewRepository.findByLocation_IdOrderByCreatedAtDesc(locationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminReviewResponse> getReviewsForAdmin() {
        return reviewRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toAdminResponse)
                .toList();
    }

    @Transactional
    public void removeReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Review not found"));
        reviewRepository.delete(review);
    }

    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getBooking() == null ? null : review.getBooking().getId(),
                review.getLocation().getId(),
                review.getUser().getName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt());
    }

    private AdminReviewResponse toAdminResponse(Review review) {
        return new AdminReviewResponse(
                review.getId(),
                review.getBooking() == null ? null : review.getBooking().getId(),
                review.getLocation().getId(),
                review.getLocation().getName(),
                review.getUser().getName(),
                review.getUser().getEmail(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt());
    }
}
