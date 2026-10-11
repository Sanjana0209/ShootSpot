package com.shootspot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private UserRepository userRepository;

    private ReviewService reviewService;
    private User user;
    private Booking booking;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(
                reviewRepository, bookingRepository, locationRepository, userRepository);
        user = new User();
        user.setId(7L);
        user.setName("ShootSpot User");
        user.setEmail("user@example.com");

        Location location = new Location();
        location.setId(15L);
        location.setName("Studio");

        booking = new Booking();
        booking.setUser(user);
        booking.setLocation(location);
        booking.setBookingDate(LocalDate.now().minusDays(1));
        booking.setStartTime(LocalTime.of(10, 0));
        booking.setEndTime(LocalTime.of(11, 0));
        booking.setDurationHours(1);
        booking.setTotalPrice(new BigDecimal("800.00"));
        booking.setStatus("COMPLETED");
    }

    @Test
    void createsReviewForOwnersCompletedBookingAndTrimsText() {
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(bookingRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(booking));
        when(locationRepository.findByIdForUpdate(15L))
                .thenReturn(Optional.of(booking.getLocation()));
        when(reviewRepository.existsByBooking_Id(21L)).thenReturn(false);
        when(reviewRepository.existsByUser_IdAndLocation_Id(7L, 15L)).thenReturn(false);
        when(reviewRepository.saveAndFlush(any(Review.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponse response = reviewService.createReview(
                21L, new CreateReviewRequest(5, "  Great location!  "), "user@example.com");

        ArgumentCaptor<Review> savedReview = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).saveAndFlush(savedReview.capture());
        assertEquals(user, savedReview.getValue().getUser());
        assertEquals(booking.getLocation(), savedReview.getValue().getLocation());
        assertEquals(5, response.rating());
        assertEquals("Great location!", response.comment());
    }

    @Test
    void rejectsReviewForAnotherUsersBooking() {
        User otherUser = new User();
        otherUser.setId(8L);
        when(userRepository.findByEmailIgnoreCase("other@example.com")).thenReturn(Optional.of(otherUser));
        when(bookingRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(booking));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> reviewService.createReview(
                        21L, new CreateReviewRequest(5, "Great location"), "other@example.com"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(reviewRepository, never()).saveAndFlush(any(Review.class));
    }

    @Test
    void rejectsReviewBeforeBookingIsCompleted() {
        booking.setStatus("CONFIRMED");
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(bookingRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(booking));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> reviewService.createReview(
                        21L, new CreateReviewRequest(5, "Great location"), "user@example.com"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(reviewRepository, never()).saveAndFlush(any(Review.class));
    }

    @Test
    void rejectsSecondReviewForSameBooking() {
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(bookingRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(booking));
        when(locationRepository.findByIdForUpdate(15L))
                .thenReturn(Optional.of(booking.getLocation()));
        when(reviewRepository.existsByBooking_Id(21L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> reviewService.createReview(
                        21L, new CreateReviewRequest(5, "Great location"), "user@example.com"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(reviewRepository, never()).saveAndFlush(any(Review.class));
    }

    @Test
    void createsLocationReviewForAuthenticatedUserAndTrimsText() {
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(locationRepository.findByIdForUpdate(15L))
                .thenReturn(Optional.of(booking.getLocation()));
        when(reviewRepository.existsByUser_IdAndLocation_Id(7L, 15L)).thenReturn(false);
        when(reviewRepository.saveAndFlush(any(Review.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponse response = reviewService.createLocationReview(
                15L, new CreateReviewRequest(4, "  Nice place!  "), "user@example.com");

        ArgumentCaptor<Review> savedReview = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).saveAndFlush(savedReview.capture());
        assertNull(savedReview.getValue().getBooking());
        assertEquals(user, savedReview.getValue().getUser());
        assertEquals(booking.getLocation(), savedReview.getValue().getLocation());
        assertEquals("Nice place!", response.comment());
        assertEquals("ShootSpot User", response.authorName());
    }

    @Test
    void rejectsDuplicateReviewBySameUserForLocation() {
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(locationRepository.findByIdForUpdate(15L))
                .thenReturn(Optional.of(booking.getLocation()));
        when(reviewRepository.existsByUser_IdAndLocation_Id(7L, 15L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> reviewService.createLocationReview(
                        15L, new CreateReviewRequest(5, "Great location"), "user@example.com"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(reviewRepository, never()).saveAndFlush(any(Review.class));
    }

    @Test
    void rejectsReviewSubmissionWithoutAuthenticatedUser() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> reviewService.createLocationReview(
                        15L, new CreateReviewRequest(5, "Great location"), "missing@example.com"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(locationRepository, never()).findByIdForUpdate(15L);
    }

    @Test
    void rejectsAdminAccountReviewSubmission() {
        user.setRole("ADMIN");
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> reviewService.createLocationReview(
                        15L, new CreateReviewRequest(5, "Great location"), "user@example.com"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(locationRepository, never()).findByIdForUpdate(15L);
    }

    @Test
    void rejectsReviewTextThatIsOnlyWhitespaceAfterTrimming() {
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(bookingRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(booking));
        when(locationRepository.findByIdForUpdate(15L))
                .thenReturn(Optional.of(booking.getLocation()));
        when(reviewRepository.existsByBooking_Id(21L)).thenReturn(false);
        when(reviewRepository.existsByUser_IdAndLocation_Id(7L, 15L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> reviewService.createReview(
                        21L, new CreateReviewRequest(5, "   "), "user@example.com"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(reviewRepository, never()).saveAndFlush(any(Review.class));
    }
}
