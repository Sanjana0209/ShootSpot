package com.shootspot.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shootspot.entity.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUser_IdOrderByBookingDateDescStartTimeDesc(Long userId);

    List<Booking> findAllByOrderByIdDesc();

    long countByStatus(String status);

    List<Booking> findByLocation_IdAndBookingDateAndStatusInOrderByStartTimeAsc(
            Long locationId, LocalDate bookingDate, Collection<String> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select booking from Booking booking where booking.id = :id")
    java.util.Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    @Query(value = """
            select exists (
                select 1
                from bookings booking
                where booking.location_id = :locationId
                  and booking.booking_date = :bookingDate
                  and booking.status in ('PENDING', 'CONFIRMED')
                  and booking.start_time < :requestedEndTime
                  and booking.end_time > :requestedStartTime
                  and (:excludedBookingId is null or booking.id <> :excludedBookingId)
            )
            """, nativeQuery = true)
    boolean existsOverlappingActiveBooking(
            @Param("locationId") Long locationId,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("requestedEndTime") LocalTime requestedEndTime,
            @Param("requestedStartTime") LocalTime requestedStartTime,
            @Param("excludedBookingId") Long excludedBookingId);
}
