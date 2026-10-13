package com.shootspot.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.shootspot.dto.BookingResponse;
import com.shootspot.dto.BookingAvailabilityResponse;
import com.shootspot.dto.CreateBookingRequest;
import com.shootspot.dto.AdminBookingResponse;
import com.shootspot.entity.Booking;
import com.shootspot.entity.Location;
import com.shootspot.entity.User;
import com.shootspot.repository.BookingRepository;
import com.shootspot.repository.LocationRepository;
import com.shootspot.repository.UserRepository;
import com.shootspot.repository.ReviewRepository;

@Service
public class BookingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookingService.class);
    private static final String PENDING = "PENDING";
    private static final String CONFIRMED = "CONFIRMED";

    private final BookingRepository bookingRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    public BookingService(
            BookingRepository bookingRepository,
            LocationRepository locationRepository,
            UserRepository userRepository,
            ReviewRepository reviewRepository) {
        this.bookingRepository = bookingRepository;
        this.locationRepository = locationRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request, String authenticatedEmail) {
        User user = userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .orElseThrow(() -> {
                    LOGGER.warn("Authentication rejected for POST /api/bookings: user not found");
                    return new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED, "Authentication required");
                });

        Location location = locationRepository.findByIdForUpdate(request.locationId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Location not found"));

        if (!location.isAvailable()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "This location is currently unavailable");
        }
        if (!location.isBookable()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This sample listing is not bookable. Only verified locations can accept booking requests.");
        }
        if (location.getPricePerHour() == null
                || location.getPricePerHour().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "This location does not have a valid hourly price");
        }

        LocalDate date = request.bookingDate();
        LocalTime startTime = request.startTime();
        LocalTime endTime = startTime.plusHours(request.durationHours());

        if (!endTime.isAfter(startTime)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "A booking must end before midnight");
        }
        if (!LocalDateTime.of(date, startTime).isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Booking start time must be in the future");
        }

        boolean overlaps = bookingRepository.existsOverlappingActiveBooking(
                location.getId(), date, endTime, startTime, null);
        if (overlaps) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Already booked for this time slot. Please choose another time.");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setLocation(location);
        booking.setBookingDate(date);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setDurationHours(request.durationHours());
        booking.setTotalPrice(location.getPricePerHour()
                .multiply(BigDecimal.valueOf(request.durationHours())));
        booking.setStatus(PENDING);

        return toResponse(bookingRepository.saveAndFlush(booking), false);
    }

    @Transactional(readOnly = true)
    public BookingAvailabilityResponse getAvailability(
            Long locationId, LocalDate bookingDate, LocalTime startTime, int durationHours) {
        if (durationHours < 1 || durationHours > 12) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Duration must be between 1 and 12 hours");
        }

        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Location not found"));
        if (!location.isBookable()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "This sample listing is not bookable.");
        }
        if (!location.isAvailable()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "This location is currently unavailable");
        }

        List<Booking> activeBookings =
                bookingRepository.findByLocation_IdAndBookingDateAndStatusInOrderByStartTimeAsc(
                        locationId, bookingDate, List.of(PENDING, CONFIRMED));
        List<BookingAvailabilityResponse.OccupiedInterval> occupiedIntervals =
                activeBookings.stream()
                        .map(booking -> new BookingAvailabilityResponse.OccupiedInterval(
                                booking.getStartTime(), booking.getEndTime()))
                        .toList();

        LocalTime candidate = startTime;
        while (isValidSameDaySlot(candidate, durationHours)) {
            LocalTime candidateEnd = candidate.plusHours(durationHours);
            Booking overlappingBooking = null;
            for (Booking activeBooking : activeBookings) {
                if (activeBooking.getStartTime().isBefore(candidateEnd)
                        && activeBooking.getEndTime().isAfter(candidate)) {
                    overlappingBooking = activeBooking;
                    break;
                }
            }

            if (overlappingBooking == null) {
                boolean requestedSlotAvailable = candidate.equals(startTime);
                return new BookingAvailabilityResponse(
                        occupiedIntervals,
                        requestedSlotAvailable,
                        requestedSlotAvailable ? null : candidate);
            }
            candidate = overlappingBooking.getEndTime();
        }

        return new BookingAvailabilityResponse(occupiedIntervals, false, null);
    }

    private boolean isValidSameDaySlot(LocalTime startTime, int durationHours) {
        return startTime.plusHours(durationHours).isAfter(startTime);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getCurrentUserBookings(String authenticatedEmail) {
        User user = userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authentication required"));
        List<Booking> bookings = bookingRepository
                .findByUser_IdOrderByBookingDateDescStartTimeDesc(user.getId());
        if (bookings.isEmpty()) {
            return List.of();
        }
        Set<Long> bookingIds = bookings.stream()
                .map(Booking::getId)
                .collect(Collectors.toSet());
        Set<Long> reviewedBookingIds = Set.copyOf(
                reviewRepository.findReviewedBookingIds(bookingIds));
        return bookings.stream()
                .map(booking -> toResponse(booking, reviewedBookingIds.contains(booking.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminBookingResponse> getAllBookingsForAdmin() {
        return bookingRepository.findAllByOrderByIdDesc()
                .stream()
                .map(this::toAdminResponse)
                .toList();
    }

    @Transactional
    public AdminBookingResponse updateBookingStatus(Long bookingId, String requestedStatus) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found"));
        String currentStatus = booking.getStatus();
        if (currentStatus.equals(requestedStatus)) {
            return toAdminResponse(booking);
        }

        boolean validTransition = switch (currentStatus) {
            case PENDING -> requestedStatus.equals(CONFIRMED)
                    || requestedStatus.equals("CANCELLED");
            case CONFIRMED -> requestedStatus.equals("CANCELLED")
                    || requestedStatus.equals("COMPLETED");
            default -> false;
        };
        if (!validTransition) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Booking status cannot change from " + currentStatus + " to " + requestedStatus);
        }

        if (requestedStatus.equals("COMPLETED")
                && LocalDateTime.of(booking.getBookingDate(), booking.getEndTime())
                        .isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "A booking can only be completed after its end time");
        }

        if (requestedStatus.equals(CONFIRMED)) {
            Location location = locationRepository.findByIdForUpdate(booking.getLocation().getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Location not found"));
            if (!location.isAvailable()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "This location is currently unavailable");
            }
            boolean overlaps = bookingRepository.existsOverlappingActiveBooking(
                    location.getId(),
                    booking.getBookingDate(),
                    booking.getEndTime(),
                    booking.getStartTime(),
                    booking.getId());
            if (overlaps) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "This time overlaps another pending or confirmed booking");
            }
        }

        booking.setStatus(requestedStatus);
        return toAdminResponse(bookingRepository.saveAndFlush(booking));
    }

    private BookingResponse toResponse(Booking booking, boolean reviewed) {
        Location location = booking.getLocation();
        return new BookingResponse(
                booking.getId(),
                location.getId(),
                location.getName(),
                location.getCity(),
                booking.getBookingDate(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getDurationHours(),
                booking.getTotalPrice(),
                booking.getStatus(),
                reviewed);
    }

    private AdminBookingResponse toAdminResponse(Booking booking) {
        Location location = booking.getLocation();
        User user = booking.getUser();
        return new AdminBookingResponse(
                booking.getId(),
                location.getId(),
                location.getName(),
                location.getCity(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                booking.getBookingDate(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getDurationHours(),
                booking.getTotalPrice(),
                booking.getStatus());
    }
}
