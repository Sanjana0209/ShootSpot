package com.shootspot.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shootspot.entity.Booking;
import com.shootspot.entity.Location;
import com.shootspot.entity.User;
import com.shootspot.repository.BookingRepository;
import com.shootspot.repository.LocationRepository;
import com.shootspot.repository.UserRepository;
import com.shootspot.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookingConcurrencyIntegrationTest {

    private static final String TEST_JWT_SECRET =
            Base64.getEncoder().encodeToString(
                    "shootspot-isolated-booking-test-secret-32-bytes".getBytes());

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureIsolatedDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("app.jwt.secret", () -> TEST_JWT_SECRET);
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @LocalServerPort
    private int port;

    private HttpClient httpClient;
    private TestUser firstUser;
    private TestUser secondUser;

    @BeforeEach
    void setUpUsers() {
        httpClient = HttpClient.newHttpClient();
        firstUser = createUser();
        secondUser = createUser();
    }

    @Test
    void concurrentOverlappingRequestsFromDifferentUsersAllowOnlyOneBooking()
            throws Exception {
        Location location = createLocation();
        LocalDate bookingDate = LocalDate.now().plusDays(2);
        HttpRequest firstRequest = bookingRequest(location, bookingDate, LocalTime.of(10, 0), 2, firstUser);
        HttpRequest secondRequest = bookingRequest(location, bookingDate, LocalTime.of(10, 0), 2, secondUser);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<HttpResponse<String>> firstResponse = executor.submit(
                    () -> sendTogether(firstRequest, ready, start));
            Future<HttpResponse<String>> secondResponse = executor.submit(
                    () -> sendTogether(secondRequest, ready, start));

            assertTrue(ready.await(5, TimeUnit.SECONDS), "Both booking requests should be ready");
            start.countDown();

            int firstStatus = firstResponse.get(30, TimeUnit.SECONDS).statusCode();
            int secondStatus = secondResponse.get(30, TimeUnit.SECONDS).statusCode();
            assertEquals(1, countStatus(firstStatus, secondStatus, HttpStatus.CREATED.value()));
            assertEquals(1, countStatus(firstStatus, secondStatus, HttpStatus.CONFLICT.value()));
            Long persistedBookings = jdbcTemplate.queryForObject(
                    "select count(*) from bookings where location_id = ? and booking_date = ?",
                    Long.class,
                    location.getId(),
                    bookingDate);
            assertEquals(1L, persistedBookings);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void adjacentSlotsAreAllowed() throws Exception {
        Location location = createLocation();
        LocalDate bookingDate = LocalDate.now().plusDays(2);

        HttpResponse<String> firstResponse = httpClient.send(
                bookingRequest(location, bookingDate, LocalTime.of(10, 0), 2, firstUser),
                HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> adjacentResponse = httpClient.send(
                bookingRequest(location, bookingDate, LocalTime.of(12, 0), 2, secondUser),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(HttpStatus.CREATED.value(), firstResponse.statusCode());
        assertEquals(HttpStatus.CREATED.value(), adjacentResponse.statusCode());
    }

    @Test
    void cancelledBookingDoesNotBlockItsFormerSlot() throws Exception {
        Location location = createLocation();
        LocalDate bookingDate = LocalDate.now().plusDays(2);
        saveBooking(location, firstUser.user(), bookingDate,
                LocalTime.of(10, 0), LocalTime.of(12, 0), "CANCELLED");

        HttpResponse<String> response = httpClient.send(
                bookingRequest(location, bookingDate, LocalTime.of(10, 0), 2, secondUser),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(HttpStatus.CREATED.value(), response.statusCode());
    }

    @Test
    void overlappingPendingBookingIsRejectedWithConflict() throws Exception {
        assertExistingBookingRejected("PENDING");
    }

    @Test
    void overlappingConfirmedBookingIsRejectedWithConflict() throws Exception {
        assertExistingBookingRejected("CONFIRMED");
    }

    private void assertExistingBookingRejected(String status) throws Exception {
        Location location = createLocation();
        LocalDate bookingDate = LocalDate.now().plusDays(2);
        saveBooking(location, firstUser.user(), bookingDate,
                LocalTime.of(10, 0), LocalTime.of(12, 0), status);

        HttpResponse<String> response = httpClient.send(
                bookingRequest(location, bookingDate, LocalTime.of(10, 0), 2, secondUser),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(HttpStatus.CONFLICT.value(), response.statusCode());
    }

    private HttpResponse<String> sendTogether(
            HttpRequest request, CountDownLatch ready, CountDownLatch start)
            throws IOException, InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting to start concurrent booking requests");
        }
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest bookingRequest(
            Location location, LocalDate bookingDate, LocalTime startTime, int duration, TestUser user) {
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(new BookingPayload(
                    location.getId(), bookingDate, startTime, duration));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not serialize the booking test request", exception);
        }
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/bookings"))
                .header("Authorization", "Bearer " + user.token())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    }

    private TestUser createUser() {
        User user = new User();
        user.setName("Integration Test User");
        user.setEmail("booking-test-" + UUID.randomUUID() + "@shootspot.test");
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setRole("USER");
        user.setTokenVersion(0);
        User savedUser = userRepository.saveAndFlush(user);
        return new TestUser(savedUser, jwtService.createToken(savedUser));
    }

    private Location createLocation() {
        Location location = new Location();
        location.setName("Integration Test Location");
        location.setDescription("Disposable location for isolated booking integration tests");
        location.setAddress("Test address");
        location.setCity("Test city");
        location.setCategory("Studio");
        location.setPricePerHour(new BigDecimal("100.00"));
        location.setAvailable(true);
        return locationRepository.saveAndFlush(location);
    }

    private void saveBooking(
            Location location,
            User user,
            LocalDate date,
            LocalTime start,
            LocalTime end,
            String status) {
        Booking booking = new Booking();
        booking.setLocation(location);
        booking.setUser(user);
        booking.setBookingDate(date);
        booking.setStartTime(start);
        booking.setEndTime(end);
        booking.setDurationHours((int) (end.toSecondOfDay() - start.toSecondOfDay()) / 3600);
        booking.setTotalPrice(location.getPricePerHour()
                .multiply(BigDecimal.valueOf(booking.getDurationHours())));
        booking.setStatus(status);
        bookingRepository.saveAndFlush(booking);
    }

    private int countStatus(int first, int second, int expected) {
        return (first == expected ? 1 : 0) + (second == expected ? 1 : 0);
    }

    private record BookingPayload(
            Long locationId, LocalDate bookingDate, LocalTime startTime, int durationHours) {
    }

    private record TestUser(User user, String token) {
    }
}
