package com.shootspot.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shootspot.entity.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByBooking_Id(Long bookingId);

    boolean existsByUser_IdAndLocation_Id(Long userId, Long locationId);

    List<Review> findByLocation_IdOrderByCreatedAtDesc(Long locationId);

    List<Review> findAllByOrderByCreatedAtDesc();

    @Query("select review.booking.id from Review review where review.booking.id in :bookingIds")
    List<Long> findReviewedBookingIds(@Param("bookingIds") Collection<Long> bookingIds);

    @Query("""
            select review.location.id as locationId,
                   avg(review.rating) as averageRating,
                   count(review.id) as reviewCount
            from Review review
            group by review.location.id
            """)
    List<LocationRatingSummary> findLocationRatingSummaries();
}
