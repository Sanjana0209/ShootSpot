package com.shootspot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
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

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReviewRepository reviewRepository;

    private BookingService bookingService;
    private User authenticatedUser;
    private Location location;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(
                bookingRepository, locationRepository, userRepository, reviewRepository);

        authenticatedUser = new User();
        authenticatedUser.setId(7L);
        authenticatedUser.setEmail("user@example.com");
        location = new Location();
        location.setId(15L);
        location.setName("Studio");
        location.setCity("Hyderabad");
        location.setPricePerHour(new BigDecimal("800.00"));
        location.setAvailable(true);
        location.setBookable(true);
    }

    @Test
    void createsPendingBookingForAuthenticatedUserAtStoredPrice() {
        when(userRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(authenticatedUser));
        when(locationRepository.findByIdForUpdate(15L)).thenReturn(Optional.of(location));
        stubOverlapQuery(List.of());
        when(bookingRepository.saveAndFlush(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.createBooking(
                new CreateBookingRequest(
                        15L, LocalDate.now().plusDays(2), LocalTime.of(10, 0), 2),
                "user@example.com");

        ArgumentCaptor<Booking> savedBooking = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).saveAndFlush(savedBooking.capture());
        assertEquals(authenticatedUser, savedBooking.getValue().getUser());
        assertEquals(location, savedBooking.getValue().getLocation());
        assertEquals(LocalTime.of(12, 0), response.endTime());
        assertEquals(new BigDecimal("1600.00"), response.totalPrice());
        assertEquals("PENDING", response.status());
        assertFalse(response.reviewed());
        verify(locationRepository).findByIdForUpdate(15L);
    }

    @Test
    void adminBookingListReturnsNewestCreatedBookingFirst() {
        Booking newestBooking = createBooking(22L, "PENDING", LocalTime.of(10, 0), LocalTime.of(12, 0));
        Booking olderBooking = createBooking(21L, "CONFIRMED", LocalTime.of(13, 0), LocalTime.of(15, 0));
        when(bookingRepository.findAllByOrderByIdDesc()).thenReturn(List.of(newestBooking, olderBooking));

        List<AdminBookingResponse> response = bookingService.getAllBookingsForAdmin();

        assertEquals(List.of(22L, 21L), response.stream().map(AdminBookingResponse::id).toList());
        verify(bookingRepository).findAllByOrderByIdDesc();
    }

    @Test
    void rejectsBookingRequestForNonBookableDemoLocation() {
        when(userRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(authenticatedUser));
        when(locationRepository.findByIdForUpdate(15L)).thenReturn(Optional.of(location));
        location.setDescription("DEMO ONLY: Sample, unverified listing.");
        location.setBookable(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.createBooking(
                        new CreateBookingRequest(
                                15L, LocalDate.now().plusDays(2), LocalTime.of(10, 0), 2),
                        "user@example.com"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(bookingRepository, never()).existsOverlappingActiveBooking(
                anyLong(),
                any(LocalDate.class),
                any(LocalTime.class),
                any(LocalTime.class),
                nullable(Long.class));
        verify(bookingRepository, never()).saveAndFlush(any(Booking.class));
    }

    @Test
    void newLocationDefaultsToNotBookable() {
        assertFalse(new Location().isBookable());
    }

    @Test
    void secondUserCannotCreateOverlappingPendingBooking() {
        User secondUser = new User();
        secondUser.setId(8L);
        secondUser.setEmail("second@example.com");
        when(userRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(authenticatedUser));
        when(userRepository.findByEmailIgnoreCase("second@example.com"))
                .thenReturn(Optional.of(secondUser));
        when(locationRepository.findByIdForUpdate(15L)).thenReturn(Optional.of(location));

        List<Booking> savedBookings = new java.util.ArrayList<>();
        when(bookingRepository.existsOverlappingActiveBooking(
                anyLong(),
                any(LocalDate.class),
                any(LocalTime.class),
                any(LocalTime.class),
                nullable(Long.class)))
                .thenAnswer(invocation -> overlaps(
                        savedBookings,
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        invocation.getArgument(2),
                        invocation.getArgument(3),
                        invocation.getArgument(4)));
        when(bookingRepository.saveAndFlush(any(Booking.class)))
                .thenAnswer(invocation -> {
                    Booking saved = invocation.getArgument(0);
                    setId(saved, (long) savedBookings.size() + 1);
                    savedBookings.add(saved);
                    return saved;
                });

        CreateBookingRequest sameSlot = new CreateBookingRequest(
                15L, LocalDate.now().plusDays(2), LocalTime.of(10, 0), 2);
        BookingResponse firstResponse = bookingService.createBooking(sameSlot, "user@example.com");
        ResponseStatusException conflict = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.createBooking(sameSlot, "second@example.com"));

        assertEquals("PENDING", firstResponse.status());
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
        verify(bookingRepository).saveAndFlush(any(Booking.class));
        InOrder order = inOrder(locationRepository, bookingRepository);
        order.verify(locationRepository).findByIdForUpdate(15L);
        order.verify(bookingRepository).existsOverlappingActiveBooking(
                eq(15L),
                eq(sameSlot.bookingDate()),
                eq(LocalTime.of(12, 0)),
                eq(LocalTime.of(10, 0)),
                eq(null));
        order.verify(bookingRepository).saveAndFlush(any(Booking.class));
    }

    @Test
    void rejectsExactOverlapWithPendingBooking() {
        assertCreateRejectedFor(List.of(
                createBooking(100L, "PENDING", LocalTime.of(10, 0), LocalTime.of(12, 0))));
    }

    @Test
    void rejectsPartialOverlapOnRequestedStart() {
        assertCreateRejectedFor(List.of(
                createBooking(100L, "CONFIRMED", LocalTime.of(9, 0), LocalTime.of(11, 0))));
    }

    @Test
    void rejectsPartialOverlapOnRequestedEnd() {
        assertCreateRejectedFor(List.of(
                createBooking(101L, "PENDING", LocalTime.of(11, 0), LocalTime.of(13, 0))));
    }

    @Test
    void rejectsExistingBookingThatContainsRequestedInterval() {
        assertCreateRejectedFor(List.of(
                createBooking(100L, "CONFIRMED", LocalTime.of(9, 0), LocalTime.of(13, 0))));
    }

    @Test
    void rejectsExistingBookingContainedByRequestedInterval() {
        assertCreateRejectedFor(List.of(
                createBooking(100L, "PENDING", LocalTime.of(10, 30), LocalTime.of(11, 30))));
    }

    private void assertCreateRejectedFor(List<Booking> existingBookings) {
        when(userRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(authenticatedUser));
        when(locationRepository.findByIdForUpdate(15L)).thenReturn(Optional.of(location));
        stubOverlapQuery(existingBookings);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.createBooking(
                        new CreateBookingRequest(
                                15L, LocalDate.now().plusDays(2), LocalTime.of(10, 0), 2),
                        "user@example.com"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Already booked for this time slot. Please choose another time.",
                exception.getReason());
        verify(bookingRepository, never()).saveAndFlush(any(Booking.class));
    }

    @Test
    void allowsAdjacentBookings() {
        when(userRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(authenticatedUser));
        when(locationRepository.findByIdForUpdate(15L)).thenReturn(Optional.of(location));
        stubOverlapQuery(List.of(
                createBooking(100L, "CONFIRMED", LocalTime.of(8, 0), LocalTime.of(10, 0)),
                createBooking(101L, "PENDING", LocalTime.of(12, 0), LocalTime.of(14, 0))));
        when(bookingRepository.saveAndFlush(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.createBooking(
                new CreateBookingRequest(
                        15L, LocalDate.now().plusDays(2), LocalTime.of(10, 0), 2),
                "user@example.com");

        assertEquals("PENDING", response.status());
        verify(bookingRepository).saveAndFlush(any(Booking.class));
    }

    @Test
    void cancelledDifferentLocationAndDifferentDateBookingsDoNotBlockSlot() {
        when(userRepository.findByEmailIgnoreCase("user@example.com"))
                .thenReturn(Optional.of(authenticatedUser));
        when(locationRepository.findByIdForUpdate(15L)).thenReturn(Optional.of(location));
        Booking cancelled = createBooking(
                100L, "CANCELLED", LocalTime.of(10, 0), LocalTime.of(12, 0));
        Booking otherLocation = createBooking(
                101L, "CONFIRMED", LocalTime.of(10, 0), LocalTime.of(12, 0));
        Location differentLocation = new Location();
        differentLocation.setId(99L);
        otherLocation.setLocation(differentLocation);
        Booking differentDate = createBooking(
                102L,
                "CONFIRMED",
                LocalDate.now().plusDays(3),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0));
        stubOverlapQuery(List.of(cancelled, otherLocation, differentDate));
        when(bookingRepository.saveAndFlush(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.createBooking(
                new CreateBookingRequest(
                        15L, LocalDate.now().plusDays(2), LocalTime.of(10, 0), 2),
                "user@example.com");

        assertEquals("PENDING", response.status());
        verify(bookingRepository).saveAndFlush(any(Booking.class));
    }

    @Test
    void availabilityMergesConsecutiveBookingsBeforeSuggestingNextSlot() {
        LocalDate date = LocalDate.now().plusDays(2);
        when(locationRepository.findById(15L)).thenReturn(Optional.of(location));
        when(bookingRepository.findByLocation_IdAndBookingDateAndStatusInOrderByStartTimeAsc(
                eq(15L), eq(date), eq(List.of("PENDING", "CONFIRMED"))))
                .thenReturn(List.of(
                        createBooking(100L, "PENDING", LocalTime.of(10, 0), LocalTime.of(12, 0)),
                        createBooking(101L, "CONFIRMED", LocalTime.of(12, 0), LocalTime.of(14, 0))));

        BookingAvailabilityResponse response = bookingService.getAvailability(
                15L, date, LocalTime.of(10, 0), 2);

        assertEquals(2, response.occupiedIntervals().size());
        assertEquals(LocalTime.of(14, 0), response.nextAvailableTime());
        assertFalse(response.requestedSlotAvailable());
    }

    @Test
    void availabilityMarksGenuinelyFreeRequestedSlotAvailable() {
        LocalDate date = LocalDate.now().plusDays(2);
        when(locationRepository.findById(15L)).thenReturn(Optional.of(location));
        when(bookingRepository.findByLocation_IdAndBookingDateAndStatusInOrderByStartTimeAsc(
                eq(15L), eq(date), eq(List.of("PENDING", "CONFIRMED"))))
                .thenReturn(List.of());

        BookingAvailabilityResponse response = bookingService.getAvailability(
                15L, date, LocalTime.of(10, 0), 2);

        assertTrue(response.requestedSlotAvailable());
        assertNull(response.nextAvailableTime());
        assertEquals(List.of(), response.occupiedIntervals());
    }

    @Test
    void availabilityMarksOverlappingPendingSlotUnavailableAndSuggestsAfterIt() {
        LocalDate date = LocalDate.now().plusDays(2);
        when(locationRepository.findById(15L)).thenReturn(Optional.of(location));
        when(bookingRepository.findByLocation_IdAndBookingDateAndStatusInOrderByStartTimeAsc(
                eq(15L), eq(date), eq(List.of("PENDING", "CONFIRMED"))))
                .thenReturn(List.of(
                        createBooking(100L, "PENDING", LocalTime.of(9, 0), LocalTime.of(11, 0))));

        BookingAvailabilityResponse response = bookingService.getAvailability(
                15L, date, LocalTime.of(10, 0), 1);

        assertFalse(response.requestedSlotAvailable());
        assertEquals(LocalTime.of(11, 0), response.nextAvailableTime());
        assertEquals(1, response.occupiedIntervals().size());
    }

    @Test
    void availabilityAllowsSlotAdjacentToOccupiedBooking() {
        LocalDate date = LocalDate.now().plusDays(2);
        when(locationRepository.findById(15L)).thenReturn(Optional.of(location));
        when(bookingRepository.findByLocation_IdAndBookingDateAndStatusInOrderByStartTimeAsc(
                eq(15L), eq(date), eq(List.of("PENDING", "CONFIRMED"))))
                .thenReturn(List.of(
                        createBooking(100L, "CONFIRMED", LocalTime.of(8, 0), LocalTime.of(10, 0))));

        BookingAvailabilityResponse response = bookingService.getAvailability(
                15L, date, LocalTime.of(10, 0), 2);

        assertTrue(response.requestedSlotAvailable());
        assertNull(response.nextAvailableTime());
    }

    @Test
    void availabilityQueryExcludesCancelledBookings() {
        LocalDate date = LocalDate.now().plusDays(2);
        when(locationRepository.findById(15L)).thenReturn(Optional.of(location));
        when(bookingRepository.findByLocation_IdAndBookingDateAndStatusInOrderByStartTimeAsc(
                eq(15L), eq(date), eq(List.of("PENDING", "CONFIRMED"))))
                .thenReturn(List.of());

        BookingAvailabilityResponse response = bookingService.getAvailability(
                15L, date, LocalTime.of(10, 0), 2);

        assertTrue(response.requestedSlotAvailable());
        assertEquals(List.of(), response.occupiedIntervals());
        verify(bookingRepository).findByLocation_IdAndBookingDateAndStatusInOrderByStartTimeAsc(
                15L, date, List.of("PENDING", "CONFIRMED"));
    }

    @Test
    void availabilityReturnsNoSuggestionWhenRequestedDurationCannotFitBeforeMidnight() {
        LocalDate date = LocalDate.now().plusDays(2);
        when(locationRepository.findById(15L)).thenReturn(Optional.of(location));
        when(bookingRepository.findByLocation_IdAndBookingDateAndStatusInOrderByStartTimeAsc(
                eq(15L), eq(date), eq(List.of("PENDING", "CONFIRMED"))))
                .thenReturn(List.of(
                        createBooking(100L, "PENDING", LocalTime.of(22, 0), LocalTime.of(23, 0))));

        BookingAvailabilityResponse response = bookingService.getAvailability(
                15L, date, LocalTime.of(22, 0), 2);

        assertFalse(response.requestedSlotAvailable());
        assertNull(response.nextAvailableTime());
    }

    @Test
    void adminCanConfirmPendingBookingWhenTheSlotIsFree() {
        Booking booking = createBooking("PENDING");
        when(bookingRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(booking));
        when(locationRepository.findByIdForUpdate(15L)).thenReturn(Optional.of(location));
        stubOverlapQuery(List.of());
        when(bookingRepository.saveAndFlush(booking)).thenReturn(booking);

        AdminBookingResponse response = bookingService.updateBookingStatus(21L, "CONFIRMED");

        assertEquals("CONFIRMED", response.status());
        assertEquals("user@example.com", response.customerEmail());
    }

    @Test
    void adminCanCancelConfirmedBookingAndPersistReleasedStatus() {
        Booking booking = createBooking("CONFIRMED");
        when(bookingRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(booking));
        when(bookingRepository.saveAndFlush(booking)).thenReturn(booking);

        AdminBookingResponse response = bookingService.updateBookingStatus(21L, "CANCELLED");

        assertEquals("CANCELLED", response.status());
        verify(bookingRepository).saveAndFlush(booking);
    }

    @Test
    void adminCannotReopenCancelledBooking() {
        Booking booking = createBooking("CANCELLED");
        when(bookingRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(booking));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.updateBookingStatus(21L, "CONFIRMED"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(bookingRepository, never()).saveAndFlush(any(Booking.class));
    }

    @Test
    void adminCannotConfirmBookingOverlappingAnotherPendingBooking() {
        Booking booking = createBooking(21L, "PENDING", LocalTime.of(10, 0), LocalTime.of(12, 0));
        Booking otherPendingBooking = createBooking(
                22L, "PENDING", LocalTime.of(11, 0), LocalTime.of(13, 0));
        when(bookingRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(booking));
        when(locationRepository.findByIdForUpdate(15L)).thenReturn(Optional.of(location));
        stubOverlapQuery(List.of(booking, otherPendingBooking));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.updateBookingStatus(21L, "CONFIRMED"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(bookingRepository, never()).saveAndFlush(any(Booking.class));
    }

    private void stubOverlapQuery(List<Booking> existingBookings) {
        when(bookingRepository.existsOverlappingActiveBooking(
                anyLong(),
                any(LocalDate.class),
                any(LocalTime.class),
                any(LocalTime.class),
                nullable(Long.class)))
                .thenAnswer(invocation -> overlaps(
                        existingBookings,
                        invocation.getArgument(0),
                        invocation.getArgument(1),
                        invocation.getArgument(2),
                        invocation.getArgument(3),
                        invocation.getArgument(4)));
    }

    private boolean overlaps(
            List<Booking> existingBookings,
            Long locationId,
            LocalDate bookingDate,
            LocalTime requestedEnd,
            LocalTime requestedStart,
            Long excludedBookingId) {
        return existingBookings.stream().anyMatch(existing -> {
            Long existingLocationId = existing.getLocation().getId();
            return !existing.getId().equals(excludedBookingId)
                    && existingLocationId.equals(locationId)
                    && existing.getBookingDate().equals(bookingDate)
                    && List.of("PENDING", "CONFIRMED").contains(existing.getStatus())
                    && existing.getStartTime().isBefore(requestedEnd)
                    && existing.getEndTime().isAfter(requestedStart);
        });
    }

    private Booking createBooking(String status) {
        return createBooking(21L, status, LocalTime.of(10, 0), LocalTime.of(12, 0));
    }

    private Booking createBooking(Long id, String status, LocalTime startTime, LocalTime endTime) {
        return createBooking(id, status, LocalDate.now().plusDays(2), startTime, endTime);
    }

    private Booking createBooking(
            Long id,
            String status,
            LocalDate bookingDate,
            LocalTime startTime,
            LocalTime endTime) {
        Booking booking = new Booking();
        setId(booking, id);
        booking.setUser(authenticatedUser);
        booking.setLocation(location);
        booking.setBookingDate(bookingDate);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setDurationHours((int) (endTime.toSecondOfDay() - startTime.toSecondOfDay()) / 3600);
        booking.setTotalPrice(new BigDecimal("1600.00"));
        booking.setStatus(status);
        return booking;
    }

    private void setId(Booking booking, Long id) {
        try {
            Field idField = Booking.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(booking, id);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Unable to set booking ID for test", exception);
        }
    }
}
